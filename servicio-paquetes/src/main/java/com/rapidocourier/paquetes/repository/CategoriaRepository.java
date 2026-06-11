package com.rapidocourier.paquetes.repository;

import com.rapidocourier.paquetes.entity.Categoria;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CategoriaRepository extends JpaRepository<Categoria, UUID> {
    Optional<Categoria> findByNombre(String nombre);
    boolean existsByNombre(String nombre);
}
