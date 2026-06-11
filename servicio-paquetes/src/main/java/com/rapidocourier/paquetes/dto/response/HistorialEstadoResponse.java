package com.rapidocourier.paquetes.dto.response;

import com.rapidocourier.paquetes.enums.EstadoPaquete;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class HistorialEstadoResponse {
    private UUID id;
    private EstadoPaquete estadoAnterior;
    private EstadoPaquete estadoNuevo;
    private String usuarioEmail;
    private String observacion;
    private LocalDateTime fechaCambio;
}
