package com.rapidocourier.paquetes.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class EstadoRequest {

    @NotBlank(message = "El nuevo estado es obligatorio")
    @Pattern(regexp = "^(REGISTRADO|EN_ALMACEN|EN_TRANSITO|EN_DESTINO|ENTREGADO|CANCELADO)$",
             message = "Estado inválido. Use: REGISTRADO, EN_ALMACEN, EN_TRANSITO, EN_DESTINO, ENTREGADO o CANCELADO")
    private String nuevoEstado;

    @Size(max = 300, message = "La observación no puede superar 300 caracteres")
    private String observacion;
}
