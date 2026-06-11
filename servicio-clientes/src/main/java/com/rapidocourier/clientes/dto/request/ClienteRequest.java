package com.rapidocourier.clientes.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class ClienteRequest {

    @NotBlank(message = "El DNI es obligatorio")
    @Pattern(regexp = "^\\d{8}$", message = "El DNI debe tener exactamente 8 dígitos")
    private String dni;

    @NotBlank(message = "El email es obligatorio")
    @Email(message = "El email debe tener formato válido")
    private String email;

    @Pattern(regexp = "^[9]\\d{8}$", message = "El teléfono debe ser un celular peruano válido (9XXXXXXXX)")
    private String telefono;

    @Size(max = 250, message = "La dirección no puede superar 250 caracteres")
    private String direccion;
}
