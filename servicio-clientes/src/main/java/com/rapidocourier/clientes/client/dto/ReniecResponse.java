package com.rapidocourier.clientes.client.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Respuesta de la consulta por DNI. Acepta los dos formatos de proveedor:
 * - decolecta.com: first_name, first_last_name, second_last_name, document_number
 * - apis.net.pe:   nombres, apellidoPaterno, apellidoMaterno, numeroDocumento
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ReniecResponse {

    @JsonAlias("first_name")
    private String nombres;

    @JsonAlias("first_last_name")
    private String apellidoPaterno;

    @JsonAlias("second_last_name")
    private String apellidoMaterno;

    @JsonAlias("document_number")
    private String numeroDocumento;

    @JsonAlias("document_type")
    private String tipoDocumento;

    public String getNombreCompleto() {
        return Stream.of(nombres, apellidoPaterno, apellidoMaterno)
                .filter(s -> s != null && !s.isBlank())
                .collect(Collectors.joining(" "))
                .trim();
    }
}
