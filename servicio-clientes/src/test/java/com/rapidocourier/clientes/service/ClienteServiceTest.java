package com.rapidocourier.clientes.service;

import com.rapidocourier.clientes.client.ReniecClient;
import com.rapidocourier.clientes.client.dto.ReniecResponse;
import com.rapidocourier.clientes.dto.request.ClienteRequest;
import com.rapidocourier.clientes.dto.response.ClienteResponse;
import com.rapidocourier.clientes.entity.Cliente;
import com.rapidocourier.clientes.exception.ConflictException;
import com.rapidocourier.clientes.exception.ResourceNotFoundException;
import com.rapidocourier.clientes.repository.ClienteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

// Nota sobre @Spy: el lógica de mapeo de ReniecResponse → nombreCompleto está en el DTO mismo
// (getNombreCompleto()). No existe un componente auxiliar compartido entre microservicios que
// amerite @Spy (e.g., un calculador de tarifa cross-service). Por tanto solo se usan @Mock.
@ExtendWith(MockitoExtension.class)
class ClienteServiceTest {

    @Mock
    private ClienteRepository clienteRepository;

    @Mock
    private ReniecClient reniecClient;

    @InjectMocks
    private ClienteService clienteService;

    private ClienteRequest request;
    private ReniecResponse reniecResponse;
    private Cliente cliente;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(clienteService, "reniecToken", "token-test");

        request = new ClienteRequest();
        request.setDni("12345678");
        request.setEmail("juan@test.com");
        request.setTelefono("987654321");
        request.setDireccion("Av. Lima 123");

        reniecResponse = new ReniecResponse();
        reniecResponse.setNombres("JUAN CARLOS");
        reniecResponse.setApellidoPaterno("GARCIA");
        reniecResponse.setApellidoMaterno("LOPEZ");
        reniecResponse.setNumeroDocumento("12345678");

        cliente = Cliente.builder()
                .id(UUID.randomUUID())
                .dni("12345678")
                .nombreCompleto("JUAN CARLOS GARCIA LOPEZ")
                .email("juan@test.com")
                .telefono("987654321")
                .direccion("Av. Lima 123")
                .activo(true)
                .build();
    }

    @Test
    @DisplayName("Happy Path: registro exitoso de cliente con datos de RENIEC")
    void registrar_exitoso() {
        when(clienteRepository.existsByEmail(request.getEmail())).thenReturn(false);
        when(clienteRepository.existsByDni(request.getDni())).thenReturn(false);
        when(reniecClient.consultarPorDni(eq("12345678"), anyString())).thenReturn(reniecResponse);
        when(clienteRepository.save(any(Cliente.class))).thenReturn(cliente);

        ClienteResponse response = clienteService.registrar(request);

        assertThat(response).isNotNull();
        assertThat(response.getDni()).isEqualTo("12345678");
        assertThat(response.getNombreCompleto()).isEqualTo("JUAN CARLOS GARCIA LOPEZ");
        assertThat(response.getEmail()).isEqualTo("juan@test.com");
        verify(reniecClient, times(1)).consultarPorDni(anyString(), anyString());
        verify(clienteRepository, times(1)).save(any(Cliente.class));
    }

    @Test
    @DisplayName("Exception: email duplicado lanza ConflictException")
    void registrar_emailDuplicado_lanzaConflict() {
        when(clienteRepository.existsByEmail(request.getEmail())).thenReturn(true);

        ConflictException ex = assertThrows(ConflictException.class,
                () -> clienteService.registrar(request));

        assertThat(ex.getMessage()).contains("email");
        verify(reniecClient, never()).consultarPorDni(anyString(), anyString());
    }

    @Test
    @DisplayName("Exception: DNI duplicado lanza ConflictException")
    void registrar_dniDuplicado_lanzaConflict() {
        when(clienteRepository.existsByEmail(request.getEmail())).thenReturn(false);
        when(clienteRepository.existsByDni(request.getDni())).thenReturn(true);

        ConflictException ex = assertThrows(ConflictException.class,
                () -> clienteService.registrar(request));

        assertThat(ex.getMessage()).contains("DNI");
        verify(reniecClient, never()).consultarPorDni(anyString(), anyString());
    }

    @Test
    @DisplayName("Exception: cliente no encontrado por ID lanza ResourceNotFoundException")
    void obtenerPorId_noExiste_lanzaNotFound() {
        UUID id = UUID.randomUUID();
        when(clienteRepository.findById(id)).thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> clienteService.obtenerPorId(id));

        assertThat(ex.getMessage()).contains(id.toString());
    }

    @Test
    @DisplayName("Happy Path: búsqueda por texto devuelve lista")
    void buscarPorTexto_devuelveResultados() {
        when(clienteRepository.buscarPorTexto("GARCIA")).thenReturn(List.of(cliente));

        List<ClienteResponse> result = clienteService.buscarPorTexto("GARCIA");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getNombreCompleto()).contains("GARCIA");
    }

    @Test
    @DisplayName("Resultado vacío: búsqueda sin coincidencias devuelve lista vacía")
    void buscarPorTexto_sinResultados_devuelveListaVacia() {
        when(clienteRepository.buscarPorTexto("XYZ999")).thenReturn(Collections.emptyList());

        List<ClienteResponse> result = clienteService.buscarPorTexto("XYZ999");

        assertThat(result).isEmpty();
    }
}
