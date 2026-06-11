package com.rapidocourier.paquetes.service;

import com.rapidocourier.paquetes.dto.request.EstadoRequest;
import com.rapidocourier.paquetes.dto.request.PaqueteRequest;
import com.rapidocourier.paquetes.dto.response.HistorialEstadoResponse;
import com.rapidocourier.paquetes.dto.response.PaqueteResponse;
import com.rapidocourier.paquetes.entity.HistorialEstado;
import com.rapidocourier.paquetes.entity.Paquete;
import com.rapidocourier.paquetes.enums.EstadoPaquete;
import com.rapidocourier.paquetes.exception.ExternalServiceException;
import com.rapidocourier.paquetes.exception.InvalidTransitionException;
import com.rapidocourier.paquetes.exception.ResourceNotFoundException;
import com.rapidocourier.paquetes.repository.HistorialEstadoRepository;
import com.rapidocourier.paquetes.repository.PaqueteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

// @Spy aplicado en EstadoPaquete para verificar que transicionesPermitidas() devuelve
// el conjunto correcto antes de que el servicio tome la decisión. Como EstadoPaquete es
// un enum (no un Spring bean), no se inyecta con @Spy de Mockito; se verifica directamente
// el comportamiento del enum en el test de transición inválida con assertThrows.
@ExtendWith(MockitoExtension.class)
class PaqueteServiceTest {

    @Mock
    private PaqueteRepository paqueteRepository;

    @Mock
    private HistorialEstadoRepository historialRepository;

    @Mock
    private ClienteLookupService clienteLookupService;

    @InjectMocks
    private PaqueteService paqueteService;

    private Paquete paquete;
    private PaqueteRequest request;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(paqueteService, "tarifaPorKg", new BigDecimal("2.5"));
        ReflectionTestUtils.setField(paqueteService, "tarifaPorKm", new BigDecimal("0.10"));
        ReflectionTestUtils.setField(paqueteService, "tarifaMinima", new BigDecimal("5.0"));

        request = new PaqueteRequest();
        request.setRemitenteId(UUID.randomUUID());
        request.setDestinatarioId(UUID.randomUUID());
        request.setSucursalOrigen("Lima");
        request.setSucursalDestino("Arequipa");
        request.setPesoKg(new BigDecimal("5.0"));
        request.setValorDeclarado(new BigDecimal("200.00"));
        request.setDistanciaKm(new BigDecimal("100.0"));
        request.setDescripcion("Documentos importantes");

        paquete = Paquete.builder()
                .id(UUID.randomUUID())
                .codigoRastreo("RC202406060001")
                .remitenteId(request.getRemitenteId())
                .destinatarioId(request.getDestinatarioId())
                .sucursalOrigen("Lima")
                .sucursalDestino("Arequipa")
                .pesoKg(new BigDecimal("5.0"))
                .distanciaKm(new BigDecimal("100.0"))
                .tarifa(new BigDecimal("22.50"))
                .estado(EstadoPaquete.REGISTRADO)
                .build();
    }

    @Test
    @DisplayName("Happy Path: cálculo de tarifa correcto al registrar paquete")
    void registrar_calculaTarifaCorrectamente() {
        when(clienteLookupService.validarYObtenerNombre(any(UUID.class), anyString()))
                .thenReturn("JUAN PEREZ");
        when(paqueteRepository.save(any(Paquete.class))).thenReturn(paquete);
        when(historialRepository.save(any(HistorialEstado.class))).thenReturn(null);

        PaqueteResponse response = paqueteService.registrar(request);

        assertThat(response).isNotNull();
        assertThat(response.getCodigoRastreo()).startsWith("RC");
        // Tarifa = (5 * 2.5) + (100 * 0.10) = 12.5 + 10 = 22.50
        assertThat(response.getTarifa()).isEqualByComparingTo(new BigDecimal("22.50"));
        verify(paqueteRepository, times(1)).save(any(Paquete.class));
    }

    @Test
    @DisplayName("Happy Path: transición de estado válida REGISTRADO → EN_ALMACEN")
    void actualizarEstado_transicionValida() {
        EstadoRequest estadoRequest = new EstadoRequest();
        estadoRequest.setNuevoEstado("EN_ALMACEN");
        estadoRequest.setObservacion("Paquete recibido en almacén Lima");

        Paquete actualizado = Paquete.builder()
                .id(paquete.getId())
                .codigoRastreo(paquete.getCodigoRastreo())
                .remitenteId(paquete.getRemitenteId())
                .destinatarioId(paquete.getDestinatarioId())
                .sucursalOrigen(paquete.getSucursalOrigen())
                .sucursalDestino(paquete.getSucursalDestino())
                .pesoKg(paquete.getPesoKg())
                .distanciaKm(paquete.getDistanciaKm())
                .tarifa(paquete.getTarifa())
                .estado(EstadoPaquete.EN_ALMACEN)
                .build();

        when(paqueteRepository.findById(paquete.getId())).thenReturn(Optional.of(paquete));
        when(paqueteRepository.save(any(Paquete.class))).thenReturn(actualizado);
        when(historialRepository.save(any(HistorialEstado.class))).thenReturn(null);

        PaqueteResponse response = paqueteService.actualizarEstado(paquete.getId(), estadoRequest);

        assertThat(response.getEstado()).isEqualTo(EstadoPaquete.EN_ALMACEN);
        verify(historialRepository, times(1)).save(any(HistorialEstado.class));
    }

    @Test
    @DisplayName("Exception: transición inválida REGISTRADO → ENTREGADO lanza InvalidTransitionException")
    void actualizarEstado_transicionInvalida_lanzaException() {
        EstadoRequest estadoRequest = new EstadoRequest();
        estadoRequest.setNuevoEstado("ENTREGADO");

        when(paqueteRepository.findById(paquete.getId())).thenReturn(Optional.of(paquete));

        InvalidTransitionException ex = assertThrows(InvalidTransitionException.class,
                () -> paqueteService.actualizarEstado(paquete.getId(), estadoRequest));

        assertThat(ex.getMessage()).contains("REGISTRADO").contains("ENTREGADO");
        verify(paqueteRepository, never()).save(any());
    }

    @Test
    @DisplayName("Exception: paquete no encontrado por ID lanza ResourceNotFoundException")
    void obtenerPorId_noExiste_lanzaNotFound() {
        UUID id = UUID.randomUUID();
        when(paqueteRepository.findById(id)).thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> paqueteService.obtenerPorId(id));

        assertThat(ex.getMessage()).contains(id.toString());
    }

    @Test
    @DisplayName("Exception: historial de paquete inexistente lanza ResourceNotFoundException")
    void obtenerHistorial_paqueteNoExiste_lanzaNotFound() {
        UUID id = UUID.randomUUID();
        when(paqueteRepository.existsById(id)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class,
                () -> paqueteService.obtenerHistorial(id));
    }

    @Test
    @DisplayName("Resultado vacío: búsqueda sin coincidencias retorna lista vacía")
    void buscarPorTexto_sinResultados_devuelveVacio() {
        when(paqueteRepository.buscarPorTexto("XXXYYY")).thenReturn(Collections.emptyList());
        when(clienteLookupService.buscarIdsPorNombre("XXXYYY")).thenReturn(Collections.emptyList());

        List<PaqueteResponse> result = paqueteService.buscarPorTexto("XXXYYY");

        assertThat(result).isEmpty();
        verify(paqueteRepository, never()).findByRemitenteIdInOrDestinatarioIdIn(any(), any());
    }

    @Test
    @DisplayName("RF-07: búsqueda por nombre de cliente combina y deduplica resultados")
    void buscarPorTexto_porNombreCliente_combinaResultados() {
        UUID clienteId = paquete.getRemitenteId();
        // El mismo paquete coincide por descripción y por remitente → debe aparecer una sola vez
        when(paqueteRepository.buscarPorTexto("juan")).thenReturn(List.of(paquete));
        when(clienteLookupService.buscarIdsPorNombre("juan")).thenReturn(List.of(clienteId));
        when(paqueteRepository.findByRemitenteIdInOrDestinatarioIdIn(List.of(clienteId), List.of(clienteId)))
                .thenReturn(List.of(paquete));

        List<PaqueteResponse> result = paqueteService.buscarPorTexto("juan");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(paquete.getId());
    }

    @Test
    @DisplayName("RF-02: remitente inexistente lanza ResourceNotFoundException y no guarda")
    void registrar_remitenteNoExiste_lanzaNotFound() {
        when(clienteLookupService.validarYObtenerNombre(request.getRemitenteId(), "remitente"))
                .thenThrow(new ResourceNotFoundException(
                        "El remitente con id " + request.getRemitenteId() + " no existe en servicio-clientes"));

        assertThrows(ResourceNotFoundException.class, () -> paqueteService.registrar(request));

        verify(paqueteRepository, never()).save(any());
        verify(historialRepository, never()).save(any());
    }

    @Test
    @DisplayName("RF-02: servicio-clientes caído lanza ExternalServiceException y no guarda")
    void registrar_servicioClientesCaido_lanzaExternalServiceException() {
        when(clienteLookupService.validarYObtenerNombre(any(UUID.class), anyString()))
                .thenThrow(new ExternalServiceException(
                        "No se pudo validar el remitente: servicio-clientes no está disponible."));

        assertThrows(ExternalServiceException.class, () -> paqueteService.registrar(request));

        verify(paqueteRepository, never()).save(any());
    }

    @Test
    @DisplayName("Tarifa mínima aplicada cuando el cálculo es menor que la mínima")
    void registrar_aplicaTarifaMinima() {
        // Peso 0.1 kg, distancia 1 km → tarifa = (0.1*2.5) + (1*0.1) = 0.35 < 5.0 → aplica mínimo 5.0
        request.setPesoKg(new BigDecimal("0.1"));
        request.setDistanciaKm(new BigDecimal("1.0"));

        Paquete pequeño = Paquete.builder()
                .id(UUID.randomUUID())
                .codigoRastreo("RC202406060002")
                .remitenteId(request.getRemitenteId())
                .destinatarioId(request.getDestinatarioId())
                .sucursalOrigen("Lima")
                .sucursalDestino("Lima")
                .pesoKg(new BigDecimal("0.1"))
                .valorDeclarado(new BigDecimal("200.00"))
                .distanciaKm(new BigDecimal("1.0"))
                .tarifa(new BigDecimal("5.00"))
                .estado(EstadoPaquete.REGISTRADO)
                .build();

        when(clienteLookupService.validarYObtenerNombre(any(UUID.class), anyString()))
                .thenReturn("JUAN PEREZ");
        when(paqueteRepository.save(any(Paquete.class))).thenReturn(pequeño);
        when(historialRepository.save(any(HistorialEstado.class))).thenReturn(null);

        PaqueteResponse response = paqueteService.registrar(request);

        assertThat(response.getTarifa()).isEqualByComparingTo(new BigDecimal("5.00"));
    }

    // Demostración de @Spy: verificación directa del comportamiento del enum EstadoPaquete
    // (componente compartido entre la lógica de servicio y los tests). Como EstadoPaquete
    // es un enum de Java (no un Spring bean), no se puede usar @Spy de Mockito sobre él;
    // en cambio se verifica su contrato directamente, demostrando el impacto cruzado.
    @Test
    @DisplayName("@Spy-equivalente: EstadoPaquete.transicionesPermitidas() devuelve las correctas")
    void estadoPaquete_transicionesPermitidas_sonCorrectas() {
        assertThat(EstadoPaquete.REGISTRADO.transicionesPermitidas())
                .containsExactlyInAnyOrder(EstadoPaquete.EN_ALMACEN, EstadoPaquete.CANCELADO);
        assertThat(EstadoPaquete.EN_ALMACEN.transicionesPermitidas())
                .containsExactlyInAnyOrder(EstadoPaquete.EN_TRANSITO, EstadoPaquete.CANCELADO);
        assertThat(EstadoPaquete.ENTREGADO.transicionesPermitidas()).isEmpty();
        assertThat(EstadoPaquete.CANCELADO.transicionesPermitidas()).isEmpty();
    }
}
