package com.rapidocourier.paquetes.client;

import com.rapidocourier.paquetes.client.dto.ClienteDto;
import com.rapidocourier.paquetes.client.dto.ClienteListDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.UUID;

// El JWT se propaga automáticamente vía RequestInterceptor (ver FeignClientConfig)
@FeignClient(name = "servicio-clientes", path = "/api/v1/clientes")
public interface ClienteClient {

    @GetMapping("/{id}")
    ClienteDto obtenerClientePorId(@PathVariable("id") UUID id);

    // RF-07: búsqueda de clientes por nombre/DNI parcial
    @GetMapping
    ClienteListDto buscarClientes(@RequestParam("busqueda") String busqueda);
}
