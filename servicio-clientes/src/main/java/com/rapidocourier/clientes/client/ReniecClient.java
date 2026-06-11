package com.rapidocourier.clientes.client;

import com.rapidocourier.clientes.client.dto.ReniecResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(
        name = "reniec-client",
        url = "${reniec.api.url:https://api.decolecta.com/v1/reniec/dni}",
        fallback = ReniecClientFallback.class
)
public interface ReniecClient {

    @GetMapping
    ReniecResponse consultarPorDni(
            @RequestParam("numero") String dni,
            @RequestHeader("Authorization") String token
    );
}
