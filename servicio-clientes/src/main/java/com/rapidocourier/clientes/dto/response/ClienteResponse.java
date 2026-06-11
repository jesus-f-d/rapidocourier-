package com.rapidocourier.clientes.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class ClienteResponse {
    private UUID id;
    private String dni;
    private String nombreCompleto;   // Dato obtenido exclusivamente de RENIEC
    private String email;
    private String telefono;
    private String direccion;
    private boolean activo;
    private LocalDateTime createdAt;
}
