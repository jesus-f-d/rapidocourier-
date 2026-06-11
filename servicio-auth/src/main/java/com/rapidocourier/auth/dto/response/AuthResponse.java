package com.rapidocourier.auth.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.Set;
import java.util.UUID;

@Data
@Builder
public class AuthResponse {
    private UUID id;
    private String email;
    private String nombre;
    private Set<String> roles;
    private String token;
    private String tokenType;
}
