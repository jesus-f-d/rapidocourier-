package com.rapidocourier.auth.repository;

import com.rapidocourier.auth.entity.Rol;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface RolRepository extends JpaRepository<Rol, UUID> {
    Optional<Rol> findByNombre(Rol.NombreRol nombre);
}
