package com.rapidocourier.clientes.controller;

import com.rapidocourier.clientes.dto.request.ClienteRequest;
import com.rapidocourier.clientes.dto.request.ClienteUpdateRequest;
import com.rapidocourier.clientes.dto.response.ApiResponse;
import com.rapidocourier.clientes.dto.response.ClienteResponse;
import com.rapidocourier.clientes.service.ClienteService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/clientes")
public class ClienteController {

    private final ClienteService clienteService;

    public ClienteController(ClienteService clienteService) {
        this.clienteService = clienteService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERADOR')")
    public ResponseEntity<ApiResponse<ClienteResponse>> registrar(@Valid @RequestBody ClienteRequest request) {
        ClienteResponse response = clienteService.registrar(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Cliente registrado exitosamente", response));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERADOR', 'CLIENTE')")
    public ResponseEntity<ApiResponse<ClienteResponse>> obtenerPorId(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok("Cliente encontrado", clienteService.obtenerPorId(id)));
    }

    @GetMapping("/dni/{dni}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERADOR')")
    public ResponseEntity<ApiResponse<ClienteResponse>> obtenerPorDni(@PathVariable String dni) {
        return ResponseEntity.ok(ApiResponse.ok("Cliente encontrado", clienteService.obtenerPorDni(dni)));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERADOR')")
    public ResponseEntity<ApiResponse<List<ClienteResponse>>> listar(
            @RequestParam(required = false) String busqueda) {
        List<ClienteResponse> clientes = busqueda != null
                ? clienteService.buscarPorTexto(busqueda)
                : clienteService.listarTodos();
        return ResponseEntity.ok(ApiResponse.ok("Clientes obtenidos", clientes));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERADOR')")
    public ResponseEntity<ApiResponse<ClienteResponse>> actualizar(
            @PathVariable UUID id,
            @Valid @RequestBody ClienteUpdateRequest request) {
        ClienteResponse response = clienteService.actualizar(id, request);
        return ResponseEntity.ok(ApiResponse.ok("Cliente actualizado exitosamente", response));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> desactivar(@PathVariable UUID id) {
        clienteService.desactivar(id);
        return ResponseEntity.noContent().build();
    }
}
