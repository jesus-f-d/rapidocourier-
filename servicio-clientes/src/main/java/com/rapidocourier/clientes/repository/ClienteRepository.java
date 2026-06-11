package com.rapidocourier.clientes.repository;

import com.rapidocourier.clientes.entity.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ClienteRepository extends JpaRepository<Cliente, UUID> {

    Optional<Cliente> findByDni(String dni);

    boolean existsByEmail(String email);

    boolean existsByDni(String dni);

    // Búsqueda por texto parcial en nombre o DNI (RF-07)
    @Query("SELECT c FROM Cliente c WHERE " +
           "LOWER(c.nombreCompleto) LIKE LOWER(CONCAT('%', :texto, '%')) OR " +
           "c.dni LIKE CONCAT('%', :texto, '%')")
    List<Cliente> buscarPorTexto(@Param("texto") String texto);

    // Clientes activos por estado
    List<Cliente> findByActivoTrue();
}
