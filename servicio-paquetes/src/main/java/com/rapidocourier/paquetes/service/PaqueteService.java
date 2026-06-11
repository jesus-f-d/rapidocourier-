package com.rapidocourier.paquetes.service;

import com.rapidocourier.paquetes.dto.request.EstadoRequest;
import com.rapidocourier.paquetes.dto.request.PaqueteRequest;
import com.rapidocourier.paquetes.dto.response.HistorialEstadoResponse;
import com.rapidocourier.paquetes.dto.response.PaqueteResponse;
import com.rapidocourier.paquetes.entity.Categoria;
import com.rapidocourier.paquetes.entity.HistorialEstado;
import com.rapidocourier.paquetes.entity.Paquete;
import com.rapidocourier.paquetes.enums.EstadoPaquete;
import com.rapidocourier.paquetes.exception.ConflictException;
import com.rapidocourier.paquetes.exception.InvalidTransitionException;
import com.rapidocourier.paquetes.exception.ResourceNotFoundException;
import com.rapidocourier.paquetes.repository.HistorialEstadoRepository;
import com.rapidocourier.paquetes.repository.PaqueteRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RefreshScope
public class PaqueteService {

    private final PaqueteRepository paqueteRepository;
    private final HistorialEstadoRepository historialRepository;
    private final ClienteLookupService clienteLookupService;

    @Value("${tarifa.base.por-kg:2.5}")
    private BigDecimal tarifaPorKg;

    @Value("${tarifa.base.por-km:0.10}")
    private BigDecimal tarifaPorKm;

    @Value("${tarifa.base.minima:5.0}")
    private BigDecimal tarifaMinima;

    public PaqueteService(PaqueteRepository paqueteRepository,
                          HistorialEstadoRepository historialRepository,
                          ClienteLookupService clienteLookupService) {
        this.paqueteRepository = paqueteRepository;
        this.historialRepository = historialRepository;
        this.clienteLookupService = clienteLookupService;
    }

    @Transactional
    public PaqueteResponse registrar(PaqueteRequest request) {
        // RF-02: Validar que remitente y destinatario existan ANTES de guardar.
        // Lanza ResourceNotFoundException (404) si no existen,
        // ExternalServiceException (502) si servicio-clientes no responde.
        String remitenteNombre = clienteLookupService
                .validarYObtenerNombre(request.getRemitenteId(), "remitente");
        String destinatarioNombre = clienteLookupService
                .validarYObtenerNombre(request.getDestinatarioId(), "destinatario");

        // RF-03: Cálculo automático de tarifa basado en peso y distancia
        BigDecimal tarifa = calcularTarifa(request.getPesoKg(), request.getDistanciaKm());

        String codigo = generarCodigoRastreo();

        Paquete paquete = Paquete.builder()
                .codigoRastreo(codigo)
                .remitenteId(request.getRemitenteId())
                .destinatarioId(request.getDestinatarioId())
                .sucursalOrigen(request.getSucursalOrigen())
                .sucursalDestino(request.getSucursalDestino())
                .pesoKg(request.getPesoKg())
                .valorDeclarado(request.getValorDeclarado())
                .distanciaKm(request.getDistanciaKm())
                .tarifa(tarifa)
                .descripcion(request.getDescripcion())
                .estado(EstadoPaquete.REGISTRADO)
                .build();

        paquete = paqueteRepository.save(paquete);

        // RF-05: Registro inicial en historial
        registrarHistorial(paquete, null, EstadoPaquete.REGISTRADO, "Paquete registrado");

        return toResponse(paquete, remitenteNombre, destinatarioNombre);
    }

    @Transactional
    public PaqueteResponse actualizarEstado(UUID id, EstadoRequest request) {
        Paquete paquete = paqueteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Paquete no encontrado: " + id));

        EstadoPaquete estadoActual = paquete.getEstado();
        EstadoPaquete nuevoEstado = EstadoPaquete.valueOf(request.getNuevoEstado());

        // RF-04: Validación de transiciones de estado
        if (!estadoActual.puedeTransicionarA(nuevoEstado)) {
            throw new InvalidTransitionException(
                    String.format("Transición inválida: no se puede cambiar de %s a %s. " +
                                  "Transiciones permitidas desde %s: %s",
                            estadoActual, nuevoEstado, estadoActual,
                            estadoActual.transicionesPermitidas()));
        }

        EstadoPaquete estadoAnterior = paquete.getEstado();
        paquete.setEstado(nuevoEstado);
        paquete = paqueteRepository.save(paquete);

        // RF-05: Registro automático en historial
        registrarHistorial(paquete, estadoAnterior, nuevoEstado, request.getObservacion());

        return toResponse(paquete, null, null);
    }

    public List<HistorialEstadoResponse> obtenerHistorial(UUID id) {
        if (!paqueteRepository.existsById(id)) {
            throw new ResourceNotFoundException("Paquete no encontrado: " + id);
        }
        return historialRepository.findByPaqueteIdOrderByFechaCambioAsc(id).stream()
                .map(this::toHistorialResponse)
                .collect(Collectors.toList());
    }

    public List<PaqueteResponse> filtrarPorSucursalYEstado(String sucursal, String estado) {
        if (estado != null) EstadoPaquete.valueOf(estado); // valida que el estado sea válido
        return paqueteRepository.findBySucursalAndEstado(sucursal, estado).stream()
                .map(p -> toResponse(p, null, null))
                .collect(Collectors.toList());
    }

    // RF-07: búsqueda por código de rastreo, descripción o nombre de remitente/destinatario
    public List<PaqueteResponse> buscarPorTexto(String texto) {
        Map<UUID, Paquete> resultados = new LinkedHashMap<>();

        // Búsqueda local: código de rastreo y descripción
        paqueteRepository.buscarPorTexto(texto)
                .forEach(p -> resultados.put(p.getId(), p));

        // Búsqueda por nombre de cliente: obtiene IDs desde servicio-clientes
        // (degrada a lista vacía si el servicio no responde)
        List<UUID> clienteIds = clienteLookupService.buscarIdsPorNombre(texto);
        if (!clienteIds.isEmpty()) {
            paqueteRepository.findByRemitenteIdInOrDestinatarioIdIn(clienteIds, clienteIds)
                    .forEach(p -> resultados.putIfAbsent(p.getId(), p));
        }

        return resultados.values().stream()
                .map(p -> toResponse(p, null, null))
                .collect(Collectors.toList());
    }

    public PaqueteResponse obtenerPorId(UUID id) {
        Paquete paquete = paqueteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Paquete no encontrado: " + id));
        return toResponse(paquete, null, null);
    }

    public List<PaqueteResponse> listarTodos() {
        return paqueteRepository.findAll().stream()
                .map(p -> toResponse(p, null, null))
                .collect(Collectors.toList());
    }

    public List<PaqueteResponse> listarPorRemitente(UUID remitenteId) {
        return paqueteRepository.findByRemitenteId(remitenteId).stream()
                .map(p -> toResponse(p, null, null))
                .collect(Collectors.toList());
    }

    // RF-08: paquetes donde el usuario es remitente o destinatario
    public List<PaqueteResponse> listarPorUsuario(UUID userId) {
        Map<UUID, Paquete> resultados = new LinkedHashMap<>();
        paqueteRepository.findByRemitenteId(userId).forEach(p -> resultados.put(p.getId(), p));
        paqueteRepository.findByDestinatarioId(userId).forEach(p -> resultados.putIfAbsent(p.getId(), p));
        return resultados.values().stream()
                .map(p -> toResponse(p, null, null))
                .collect(Collectors.toList());
    }

    @Transactional
    public PaqueteResponse actualizar(UUID id, PaqueteRequest request) {
        Paquete paquete = paqueteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Paquete no encontrado: " + id));

        if (paquete.getEstado() != EstadoPaquete.REGISTRADO) {
            throw new ConflictException("Solo se puede editar un paquete en estado REGISTRADO");
        }

        BigDecimal nuevaTarifa = calcularTarifa(request.getPesoKg(), request.getDistanciaKm());
        paquete.setPesoKg(request.getPesoKg());
        paquete.setValorDeclarado(request.getValorDeclarado());
        paquete.setDistanciaKm(request.getDistanciaKm());
        paquete.setTarifa(nuevaTarifa);
        paquete.setDescripcion(request.getDescripcion());
        paquete.setSucursalOrigen(request.getSucursalOrigen());
        paquete.setSucursalDestino(request.getSucursalDestino());

        return toResponse(paqueteRepository.save(paquete), null, null);
    }

    @Transactional
    public void eliminar(UUID id) {
        if (!paqueteRepository.existsById(id)) {
            throw new ResourceNotFoundException("Paquete no encontrado: " + id);
        }
        paqueteRepository.deleteById(id);
    }

    // RF-03: Fórmula: tarifa = (pesoKg * tarifaPorKg) + (distanciaKm * tarifaPorKm), mínimo tarifaMinima
    private BigDecimal calcularTarifa(BigDecimal pesoKg, BigDecimal distanciaKm) {
        BigDecimal tarifaPeso = pesoKg.multiply(tarifaPorKg);
        BigDecimal tarifaDistancia = distanciaKm.multiply(tarifaPorKm);
        BigDecimal total = tarifaPeso.add(tarifaDistancia).setScale(2, RoundingMode.HALF_UP);
        return total.compareTo(tarifaMinima) < 0 ? tarifaMinima : total;
    }

    private String generarCodigoRastreo() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmm"));
        String random = String.format("%04d", (int) (Math.random() * 10000));
        return "RC" + timestamp + random;
    }

    private void registrarHistorial(Paquete paquete, EstadoPaquete anterior,
                                     EstadoPaquete nuevo, String observacion) {
        String usuario = obtenerUsuarioActual();
        HistorialEstado historial = HistorialEstado.builder()
                .paquete(paquete)
                .estadoAnterior(anterior)
                .estadoNuevo(nuevo)
                .usuarioEmail(usuario)
                .observacion(observacion)
                .build();
        historialRepository.save(historial);
    }

    private String obtenerUsuarioActual() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (auth != null && auth.isAuthenticated()) ? auth.getName() : "sistema";
    }

    private PaqueteResponse toResponse(Paquete p, String remitenteNombre, String destinatarioNombre) {
        return PaqueteResponse.builder()
                .id(p.getId())
                .codigoRastreo(p.getCodigoRastreo())
                .remitenteId(p.getRemitenteId())
                .remitenteNombre(remitenteNombre)
                .destinatarioId(p.getDestinatarioId())
                .destinatarioNombre(destinatarioNombre)
                .sucursalOrigen(p.getSucursalOrigen())
                .sucursalDestino(p.getSucursalDestino())
                .pesoKg(p.getPesoKg())
                .valorDeclarado(p.getValorDeclarado())
                .distanciaKm(p.getDistanciaKm())
                .tarifa(p.getTarifa())
                .descripcion(p.getDescripcion())
                .estado(p.getEstado())
                .categorias(p.getCategorias().stream()
                        .map(Categoria::getNombre)
                        .collect(Collectors.toSet()))
                .createdAt(p.getCreatedAt())
                .updatedAt(p.getUpdatedAt())
                .build();
    }

    private HistorialEstadoResponse toHistorialResponse(HistorialEstado h) {
        return HistorialEstadoResponse.builder()
                .id(h.getId())
                .estadoAnterior(h.getEstadoAnterior())
                .estadoNuevo(h.getEstadoNuevo())
                .usuarioEmail(h.getUsuarioEmail())
                .observacion(h.getObservacion())
                .fechaCambio(h.getFechaCambio())
                .build();
    }
}
