package com.rapidocourier.paquetes.service;

import com.rapidocourier.paquetes.entity.Categoria;
import com.rapidocourier.paquetes.entity.Paquete;
import com.rapidocourier.paquetes.exception.ConflictException;
import com.rapidocourier.paquetes.exception.ResourceNotFoundException;
import com.rapidocourier.paquetes.repository.CategoriaRepository;
import com.rapidocourier.paquetes.repository.PaqueteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class CategoriaService {

    private final CategoriaRepository categoriaRepository;
    private final PaqueteRepository paqueteRepository;

    public CategoriaService(CategoriaRepository categoriaRepository, PaqueteRepository paqueteRepository) {
        this.categoriaRepository = categoriaRepository;
        this.paqueteRepository = paqueteRepository;
    }

    public List<Categoria> listarCategorias() {
        return categoriaRepository.findAll();
    }

    public Categoria obtenerPorId(UUID id) {
        return categoriaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Categoría no encontrada: " + id));
    }

    @Transactional
    public Categoria crearCategoria(String nombre, String descripcion) {
        if (categoriaRepository.existsByNombre(nombre)) {
            throw new ConflictException("Ya existe una categoría con el nombre: " + nombre);
        }
        return categoriaRepository.save(
                Categoria.builder().nombre(nombre).descripcion(descripcion).build());
    }

    // RF-09: Asignación de categoría a paquete (relación M:M)
    @Transactional
    public void asignarCategoria(UUID paqueteId, UUID categoriaId) {
        Paquete paquete = paqueteRepository.findById(paqueteId)
                .orElseThrow(() -> new ResourceNotFoundException("Paquete no encontrado: " + paqueteId));

        Categoria categoria = categoriaRepository.findById(categoriaId)
                .orElseThrow(() -> new ResourceNotFoundException("Categoría no encontrada: " + categoriaId));

        if (paquete.getCategorias().contains(categoria)) {
            throw new ConflictException("El paquete ya tiene asignada la categoría: " + categoria.getNombre());
        }

        paquete.getCategorias().add(categoria);
        paqueteRepository.save(paquete);
    }
}
