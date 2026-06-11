package com.rapidocourier.clientes.service;

import com.rapidocourier.clientes.client.ReniecClient;
import com.rapidocourier.clientes.client.dto.ReniecResponse;
import com.rapidocourier.clientes.dto.request.ClienteRequest;
import com.rapidocourier.clientes.dto.request.ClienteUpdateRequest;
import com.rapidocourier.clientes.dto.response.ClienteResponse;
import com.rapidocourier.clientes.entity.Cliente;
import com.rapidocourier.clientes.exception.ConflictException;
import com.rapidocourier.clientes.exception.ExternalServiceException;
import com.rapidocourier.clientes.exception.ResourceNotFoundException;
import com.rapidocourier.clientes.repository.ClienteRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RefreshScope
public class ClienteService {

    private final ClienteRepository clienteRepository;
    private final ReniecClient reniecClient;

    @Value("${reniec.api.token:token-de-prueba}")
    private String reniecToken;

    public ClienteService(ClienteRepository clienteRepository, ReniecClient reniecClient) {
        this.clienteRepository = clienteRepository;
        this.reniecClient = reniecClient;
    }

    @Transactional
    @CircuitBreaker(name = "reniec-cb", fallbackMethod = "registrarClienteFallback")
    @Retry(name = "reniec-retry")
    public ClienteResponse registrar(ClienteRequest request) {
        if (clienteRepository.existsByEmail(request.getEmail())) {
            throw new ConflictException("Ya existe un cliente con el email: " + request.getEmail());
        }
        if (clienteRepository.existsByDni(request.getDni())) {
            throw new ConflictException("Ya existe un cliente con el DNI: " + request.getDni());
        }

        // RF-01: Nombre obtenido exclusivamente de RENIEC
        ReniecResponse reniec = reniecClient.consultarPorDni(request.getDni(), "Bearer " + reniecToken);

        Cliente cliente = Cliente.builder()
                .dni(request.getDni())
                .nombreCompleto(reniec.getNombreCompleto())
                .email(request.getEmail())
                .telefono(request.getTelefono())
                .direccion(request.getDireccion())
                .activo(true)
                .build();

        return toResponse(clienteRepository.save(cliente));
    }

    public ClienteResponse registrarClienteFallback(ClienteRequest request, Exception ex) {
        // Propaga excepciones de negocio (email/DNI duplicado) sin envolverlas
        if (ex instanceof ConflictException) throw (ConflictException) ex;
        // RENIEC no disponible: respuesta degradada con nombre pendiente de validar
        throw new ExternalServiceException(
                "El servicio RENIEC no está disponible en este momento. Intente nuevamente en unos minutos.");
    }

    public ClienteResponse obtenerPorId(UUID id) {
        return toResponse(clienteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado con id: " + id)));
    }

    public ClienteResponse obtenerPorDni(String dni) {
        return toResponse(clienteRepository.findByDni(dni)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado con DNI: " + dni)));
    }

    public List<ClienteResponse> buscarPorTexto(String texto) {
        return clienteRepository.buscarPorTexto(texto).stream()
                .map(this::toResponse)
                .toList();
    }

    public List<ClienteResponse> listarTodos() {
        return clienteRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public ClienteResponse actualizar(UUID id, ClienteUpdateRequest request) {
        Cliente cliente = clienteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado con id: " + id));

        if (!cliente.getEmail().equals(request.getEmail())
                && clienteRepository.existsByEmail(request.getEmail())) {
            throw new ConflictException("Ya existe un cliente con el email: " + request.getEmail());
        }

        cliente.setEmail(request.getEmail());
        cliente.setTelefono(request.getTelefono());
        cliente.setDireccion(request.getDireccion());

        return toResponse(clienteRepository.save(cliente));
    }

    @Transactional
    public void desactivar(UUID id) {
        Cliente cliente = clienteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado con id: " + id));
        cliente.setActivo(false);
        clienteRepository.save(cliente);
    }

    private ClienteResponse toResponse(Cliente c) {
        return ClienteResponse.builder()
                .id(c.getId())
                .dni(c.getDni())
                .nombreCompleto(c.getNombreCompleto())
                .email(c.getEmail())
                .telefono(c.getTelefono())
                .direccion(c.getDireccion())
                .activo(c.isActivo())
                .createdAt(c.getCreatedAt())
                .build();
    }
}
