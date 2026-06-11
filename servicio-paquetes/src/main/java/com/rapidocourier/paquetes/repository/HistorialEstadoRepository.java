package com.rapidocourier.paquetes.repository;

import com.rapidocourier.paquetes.entity.HistorialEstado;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface HistorialEstadoRepository extends JpaRepository<HistorialEstado, UUID> {
    List<HistorialEstado> findByPaqueteIdOrderByFechaCambioAsc(UUID paqueteId);
}
