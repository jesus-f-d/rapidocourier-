package com.rapidocourier.paquetes.controller;

import com.rapidocourier.paquetes.dto.response.ApiResponse;
import com.rapidocourier.paquetes.entity.Categoria;
import com.rapidocourier.paquetes.service.CategoriaService;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/categorias")
@Validated
public class CategoriaController {

    private final CategoriaService categoriaService;

    public CategoriaController(CategoriaService categoriaService) {
        this.categoriaService = categoriaService;
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Categoria>> crear(
            @RequestParam @NotBlank(message = "El nombre es obligatorio")
            @Size(max = 50, message = "El nombre no puede superar 50 caracteres") String nombre,
            @RequestParam(required = false)
            @Size(max = 200, message = "La descripción no puede superar 200 caracteres") String descripcion) {
        Categoria categoria = categoriaService.crearCategoria(nombre, descripcion);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Categoría creada exitosamente", categoria));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERADOR')")
    public ResponseEntity<ApiResponse<List<Categoria>>> listar() {
        return ResponseEntity.ok(ApiResponse.ok("Categorías obtenidas", categoriaService.listarCategorias()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERADOR')")
    public ResponseEntity<ApiResponse<Categoria>> obtenerPorId(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok("Categoría encontrada", categoriaService.obtenerPorId(id)));
    }
}
