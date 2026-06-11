package com.rapidocourier.paquetes.controller;

import com.rapidocourier.paquetes.dto.request.EstadoRequest;
import com.rapidocourier.paquetes.dto.request.PaqueteRequest;
import com.rapidocourier.paquetes.dto.response.ApiResponse;
import com.rapidocourier.paquetes.dto.response.HistorialEstadoResponse;
import com.rapidocourier.paquetes.dto.response.PaqueteResponse;
import com.rapidocourier.paquetes.service.CategoriaService;
import com.rapidocourier.paquetes.service.PaqueteService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/paquetes")
public class PaqueteController {

    private final PaqueteService paqueteService;
    private final CategoriaService categoriaService;

    public PaqueteController(PaqueteService paqueteService, CategoriaService categoriaService) {
        this.paqueteService = paqueteService;
        this.categoriaService = categoriaService;
    }

    // RF-02, RF-03: Registro de paquete con cálculo de tarifa
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERADOR')")
    public ResponseEntity<ApiResponse<PaqueteResponse>> registrar(
            @Valid @RequestBody PaqueteRequest request) {
        PaqueteResponse response = paqueteService.registrar(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Paquete registrado. Código: " + response.getCodigoRastreo(), response));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERADOR', 'CLIENTE')")
    public ResponseEntity<ApiResponse<PaqueteResponse>> obtenerPorId(
            @PathVariable UUID id, Authentication auth) {
        PaqueteResponse paquete = paqueteService.obtenerPorId(id);
        verificarPropiedadSiCliente(auth, paquete);
        return ResponseEntity.ok(ApiResponse.ok("Paquete encontrado", paquete));
    }

    // RF-04: Actualización de estado con validación de transición
    @PatchMapping("/{id}/estado")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERADOR')")
    public ResponseEntity<ApiResponse<PaqueteResponse>> actualizarEstado(
            @PathVariable UUID id,
            @Valid @RequestBody EstadoRequest request) {
        PaqueteResponse response = paqueteService.actualizarEstado(id, request);
        return ResponseEntity.ok(ApiResponse.ok("Estado actualizado exitosamente", response));
    }

    // RF-05: Historial de estados
    @GetMapping("/{id}/historial")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERADOR', 'CLIENTE')")
    public ResponseEntity<ApiResponse<List<HistorialEstadoResponse>>> obtenerHistorial(
            @PathVariable UUID id, Authentication auth) {
        if (esClienteRestringido(auth)) {
            verificarPropiedadSiCliente(auth, paqueteService.obtenerPorId(id));
        }
        List<HistorialEstadoResponse> historial = paqueteService.obtenerHistorial(id);
        return ResponseEntity.ok(ApiResponse.ok("Historial obtenido", historial));
    }

    // RF-06, RF-07: Listado con filtros opcionales
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERADOR', 'CLIENTE')")
    public ResponseEntity<ApiResponse<List<PaqueteResponse>>> listar(
            @RequestParam(required = false) String sucursal,
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) String busqueda,
            Authentication auth) {

        boolean restringido = esClienteRestringido(auth);

        List<PaqueteResponse> paquetes;

        if (busqueda != null && !busqueda.isBlank()) {
            paquetes = paqueteService.buscarPorTexto(busqueda);
        } else if (sucursal != null) {
            paquetes = paqueteService.filtrarPorSucursalYEstado(sucursal, estado);
        } else {
            paquetes = restringido
                    ? paqueteService.listarPorUsuario(extraerUserId(auth))
                    : paqueteService.listarTodos();
        }

        // RF-08: un CLIENTE solo ve los paquetes donde es remitente o destinatario
        if (restringido) {
            UUID userId = extraerUserId(auth);
            paquetes = paquetes.stream()
                    .filter(p -> esPropietario(userId, p))
                    .toList();
        }

        return ResponseEntity.ok(ApiResponse.ok("Paquetes obtenidos", paquetes));
    }

    // RF-09: Asignación de categoría (relación M:M)
    @PostMapping("/{id}/categorias/{categoriaId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERADOR')")
    public ResponseEntity<ApiResponse<Void>> asignarCategoria(
            @PathVariable UUID id,
            @PathVariable UUID categoriaId) {
        categoriaService.asignarCategoria(id, categoriaId);
        return ResponseEntity.ok(ApiResponse.ok("Categoría asignada al paquete", null));
    }

    // RF-08: PUT de datos básicos (ADMIN u OPERADOR, solo en estado REGISTRADO)
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERADOR')")
    public ResponseEntity<ApiResponse<PaqueteResponse>> actualizar(
            @PathVariable UUID id,
            @Valid @RequestBody PaqueteRequest request) {
        PaqueteResponse response = paqueteService.actualizar(id, request);
        return ResponseEntity.ok(ApiResponse.ok("Paquete actualizado exitosamente", response));
    }

    // RF-08: Solo ADMIN puede eliminar → 204 No Content
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> eliminar(@PathVariable UUID id) {
        paqueteService.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    // RF-08: CLIENTE consulta sus propios paquetes (por remitenteId = userId del JWT)
    @GetMapping("/mis-paquetes")
    @PreAuthorize("hasRole('CLIENTE')")
    public ResponseEntity<ApiResponse<List<PaqueteResponse>>> misPaquetes(Authentication auth) {
        String userId = (String) auth.getDetails();
        UUID remitenteId = userId != null ? UUID.fromString(userId) : null;
        List<PaqueteResponse> paquetes = remitenteId != null
                ? paqueteService.listarPorRemitente(remitenteId)
                : List.of();
        return ResponseEntity.ok(ApiResponse.ok("Mis paquetes obtenidos", paquetes));
    }

    // --- RF-08: helpers de propiedad para el rol CLIENTE ---

    // CLIENTE sin rol ADMIN/OPERADOR: acceso restringido a sus propios paquetes
    private boolean esClienteRestringido(Authentication auth) {
        boolean esCliente = false;
        for (var authority : auth.getAuthorities()) {
            String rol = authority.getAuthority();
            if (rol.equals("ROLE_ADMIN") || rol.equals("ROLE_OPERADOR")) return false;
            if (rol.equals("ROLE_CLIENTE")) esCliente = true;
        }
        return esCliente;
    }

    private UUID extraerUserId(Authentication auth) {
        String userId = (String) auth.getDetails();
        if (userId == null) {
            throw new AccessDeniedException("El token no contiene el identificador del usuario");
        }
        return UUID.fromString(userId);
    }

    private boolean esPropietario(UUID userId, PaqueteResponse paquete) {
        return userId.equals(paquete.getRemitenteId()) || userId.equals(paquete.getDestinatarioId());
    }

    private void verificarPropiedadSiCliente(Authentication auth, PaqueteResponse paquete) {
        if (esClienteRestringido(auth) && !esPropietario(extraerUserId(auth), paquete)) {
            throw new AccessDeniedException("El paquete no pertenece al usuario autenticado");
        }
    }
}
