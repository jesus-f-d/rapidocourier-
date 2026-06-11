package com.rapidocourier.paquetes.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class PaqueteRequest {

    @NotNull(message = "El ID del remitente es obligatorio")
    private UUID remitenteId;

    @NotNull(message = "El ID del destinatario es obligatorio")
    private UUID destinatarioId;

    @NotBlank(message = "La sucursal de origen es obligatoria")
    @Size(min = 2, max = 100, message = "La sucursal de origen debe tener entre 2 y 100 caracteres")
    private String sucursalOrigen;

    @NotBlank(message = "La sucursal de destino es obligatoria")
    @Size(min = 2, max = 100, message = "La sucursal de destino debe tener entre 2 y 100 caracteres")
    private String sucursalDestino;

    @NotNull(message = "El peso es obligatorio")
    @Positive(message = "El peso debe ser mayor a 0")
    @DecimalMax(value = "1000.000", message = "El peso no puede superar 1000 kg")
    private BigDecimal pesoKg;

    @NotNull(message = "El valor declarado es obligatorio")
    @Positive(message = "El valor declarado debe ser mayor a 0")
    @DecimalMax(value = "999999.99", message = "El valor declarado no puede superar S/. 999,999.99")
    private BigDecimal valorDeclarado;

    @NotNull(message = "La distancia es obligatoria")
    @Positive(message = "La distancia debe ser mayor a 0")
    private BigDecimal distanciaKm;

    @Size(max = 500, message = "La descripción no puede superar 500 caracteres")
    private String descripcion;
}
