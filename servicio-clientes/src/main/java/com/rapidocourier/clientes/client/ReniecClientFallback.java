package com.rapidocourier.clientes.client;

import com.rapidocourier.clientes.client.dto.ReniecResponse;
import com.rapidocourier.clientes.exception.ExternalServiceException;
import org.springframework.stereotype.Component;

@Component
public class ReniecClientFallback implements ReniecClient {

    @Override
    public ReniecResponse consultarPorDni(String dni, String token) {
        throw new ExternalServiceException(
                "El servicio RENIEC no está disponible en este momento. Intente más tarde.");
    }
}
