package com.rapidocourier.gateway.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Respuestas degradadas para los CircuitBreaker del gateway (fallbackUri).
 * Devuelve 503 con la misma estructura de ApiResponse del resto del sistema.
 * Se usa @RequestMapping sin método porque el forward conserva el verbo HTTP original.
 */
@RestController
@RequestMapping("/fallback")
public class FallbackController {

    @RequestMapping("/auth")
    public ResponseEntity<Map<String, Object>> authFallback() {
        return fallback("Servicio de autenticación no disponible temporalmente");
    }

    @RequestMapping("/clientes")
    public ResponseEntity<Map<String, Object>> clientesFallback() {
        return fallback("Servicio de clientes no disponible temporalmente");
    }

    @RequestMapping("/paquetes")
    public ResponseEntity<Map<String, Object>> paquetesFallback() {
        return fallback("Servicio de paquetes no disponible temporalmente");
    }

    private ResponseEntity<Map<String, Object>> fallback(String message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("success", false);
        body.put("message", message);
        body.put("data", null);
        body.put("timestamp", LocalDateTime.now().toString());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(body);
    }
}
