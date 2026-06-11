package com.rapidocourier.paquetes.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.UUID;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ClienteDto {
    private boolean success;
    private String message;
    private ClienteData data;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ClienteData {
        private UUID id;
        private String dni;
        private String nombreCompleto;
        private String email;
        private boolean activo;
    }
}
