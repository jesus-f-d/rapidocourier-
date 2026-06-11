package com.rapidocourier.paquetes.service;

import com.rapidocourier.paquetes.client.ClienteClient;
import com.rapidocourier.paquetes.client.dto.ClienteDto;
import com.rapidocourier.paquetes.client.dto.ClienteListDto;
import com.rapidocourier.paquetes.exception.ExternalServiceException;
import com.rapidocourier.paquetes.exception.ResourceNotFoundException;
import feign.FeignException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Encapsula las llamadas Feign a servicio-clientes en un bean público para que
 * Spring AOP pueda interceptar @CircuitBreaker (no funciona en métodos privados
 * ni auto-invocaciones).
 */
@Service
public class ClienteLookupService {

    private final ClienteClient clienteClient;

    public ClienteLookupService(ClienteClient clienteClient) {
        this.clienteClient = clienteClient;
    }

    /**
     * RF-02: valida que el cliente exista antes de registrar un paquete.
     * - Cliente inexistente → ResourceNotFoundException (404).
     * - servicio-clientes no disponible → ExternalServiceException (502).
     *
     * @return nombre completo del cliente validado
     */
    @CircuitBreaker(name = "clientes-cb", fallbackMethod = "validarClienteFallback")
    public String validarYObtenerNombre(UUID clienteId, String rolCliente) {
        try {
            ClienteDto dto = clienteClient.obtenerClientePorId(clienteId);
            if (dto.getData() == null) {
                throw new ResourceNotFoundException(
                        "El " + rolCliente + " con id " + clienteId + " no existe en servicio-clientes");
            }
            return dto.getData().getNombreCompleto();
        } catch (FeignException.NotFound e) {
            throw new ResourceNotFoundException(
                    "El " + rolCliente + " con id " + clienteId + " no existe en servicio-clientes");
        }
    }

    public String validarClienteFallback(UUID clienteId, String rolCliente, Exception ex) {
        if (ex instanceof ResourceNotFoundException notFound) throw notFound;
        throw new ExternalServiceException(
                "No se pudo validar el " + rolCliente + ": servicio-clientes no está disponible. " +
                "Intente nuevamente en unos minutos.");
    }

    /**
     * Enriquecimiento de respuesta: si servicio-clientes falla, degrada a "N/A"
     * sin interrumpir la operación.
     */
    @CircuitBreaker(name = "clientes-cb", fallbackMethod = "nombreClienteFallback")
    public String obtenerNombreCliente(UUID clienteId) {
        ClienteDto dto = clienteClient.obtenerClientePorId(clienteId);
        return dto.getData() != null ? dto.getData().getNombreCompleto() : "N/A";
    }

    public String nombreClienteFallback(UUID clienteId, Exception ex) {
        return "N/A";
    }

    /**
     * RF-07: IDs de clientes cuyo nombre/DNI coincide con el texto buscado.
     * Si servicio-clientes no responde, degrada a lista vacía (la búsqueda
     * local por código/descripción sigue funcionando).
     */
    @CircuitBreaker(name = "clientes-cb", fallbackMethod = "buscarIdsFallback")
    public List<UUID> buscarIdsPorNombre(String texto) {
        ClienteListDto dto = clienteClient.buscarClientes(texto);
        if (dto.getData() == null) return Collections.emptyList();
        return dto.getData().stream()
                .map(ClienteDto.ClienteData::getId)
                .toList();
    }

    public List<UUID> buscarIdsFallback(String texto, Exception ex) {
        return Collections.emptyList();
    }
}
