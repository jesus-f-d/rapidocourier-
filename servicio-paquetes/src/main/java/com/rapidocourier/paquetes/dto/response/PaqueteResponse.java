package com.rapidocourier.paquetes.dto.response;

import com.rapidocourier.paquetes.enums.EstadoPaquete;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

@Data
@Builder
public class PaqueteResponse {
    private UUID id;
    private String codigoRastreo;
    private UUID remitenteId;
    private String remitenteNombre;  // Dato obtenido de servicio-clientes
    private UUID destinatarioId;
    private String destinatarioNombre;  // Dato obtenido de servicio-clientes
    private String sucursalOrigen;
    private String sucursalDestino;
    private BigDecimal pesoKg;
    private BigDecimal valorDeclarado;
    private BigDecimal distanciaKm;
    private BigDecimal tarifa;
    private String descripcion;
    private EstadoPaquete estado;
    private Set<String> categorias;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
