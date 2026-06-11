package com.rapidocourier.clientes.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ClienteUpdateRequest {

    @NotBlank(message = "El email es obligatorio")
    @Email(message = "El email debe tener formato válido")
    private String email;

    @Pattern(regexp = "^[9]\\d{8}$", message = "El teléfono debe ser un celular peruano válido (9XXXXXXXX)")
    private String telefono;

    @Size(max = 250, message = "La dirección no puede superar 250 caracteres")
    private String direccion;
}
