package com.rapidocourier.paquetes.repository;

import com.rapidocourier.paquetes.entity.Paquete;
import com.rapidocourier.paquetes.enums.EstadoPaquete;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PaqueteRepository extends JpaRepository<Paquete, UUID> {

    Optional<Paquete> findByCodigoRastreo(String codigoRastreo);

    // RF-06: Filtro por sucursal (origen o destino) y estado — nativeQuery para cumplir RNF
    @Query(value = "SELECT * FROM paquetes p WHERE " +
           "(LOWER(p.sucursal_origen) LIKE LOWER(CONCAT('%', :sucursal, '%')) OR " +
           " LOWER(p.sucursal_destino) LIKE LOWER(CONCAT('%', :sucursal, '%'))) " +
           "AND (:estado IS NULL OR p.estado = :estado)",
           nativeQuery = true)
    List<Paquete> findBySucursalAndEstado(@Param("sucursal") String sucursal,
                                          @Param("estado") String estado);

    // RF-07: Búsqueda por texto parcial en código de rastreo o descripción
    @Query("SELECT p FROM Paquete p WHERE " +
           "LOWER(p.codigoRastreo) LIKE LOWER(CONCAT('%', :texto, '%')) OR " +
           "LOWER(p.descripcion) LIKE LOWER(CONCAT('%', :texto, '%'))")
    List<Paquete> buscarPorTexto(@Param("texto") String texto);

    // Consulta multi-entidad: paquetes con su historial más reciente (JOIN)
    @Query("SELECT DISTINCT p FROM Paquete p LEFT JOIN FETCH p.historial h " +
           "WHERE p.estado = :estado ORDER BY p.createdAt DESC")
    List<Paquete> findByEstadoWithHistorial(@Param("estado") EstadoPaquete estado);

    List<Paquete> findByRemitenteId(UUID remitenteId);

    List<Paquete> findByDestinatarioId(UUID destinatarioId);

    // RF-07: paquetes cuyo remitente o destinatario está en la lista de clientes
    // que coinciden con la búsqueda por nombre
    List<Paquete> findByRemitenteIdInOrDestinatarioIdIn(Collection<UUID> remitenteIds,
                                                        Collection<UUID> destinatarioIds);
}
