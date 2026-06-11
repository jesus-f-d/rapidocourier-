package com.rapidocourier.paquetes.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ClienteListDto {
    private boolean success;
    private String message;
    private List<ClienteDto.ClienteData> data;
}
