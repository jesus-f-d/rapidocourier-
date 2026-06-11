package com.rapidocourier.auth.service;

import com.rapidocourier.auth.dto.request.LoginRequest;
import com.rapidocourier.auth.dto.request.RegisterRequest;
import com.rapidocourier.auth.dto.response.AuthResponse;
import com.rapidocourier.auth.entity.Rol;
import com.rapidocourier.auth.entity.Usuario;
import com.rapidocourier.auth.exception.ConflictException;
import com.rapidocourier.auth.exception.ResourceNotFoundException;
import com.rapidocourier.auth.repository.RolRepository;
import com.rapidocourier.auth.repository.UsuarioRepository;
import com.rapidocourier.auth.security.JwtUtil;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;

    public AuthService(UsuarioRepository usuarioRepository,
                       RolRepository rolRepository,
                       PasswordEncoder passwordEncoder,
                       JwtUtil jwtUtil,
                       AuthenticationManager authenticationManager,
                       UserDetailsService userDetailsService) {
        this.usuarioRepository = usuarioRepository;
        this.rolRepository = rolRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.authenticationManager = authenticationManager;
        this.userDetailsService = userDetailsService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (usuarioRepository.existsByEmail(request.getEmail())) {
            throw new ConflictException("Ya existe un usuario con el email: " + request.getEmail());
        }

        Rol.NombreRol nombreRol = Rol.NombreRol.valueOf(request.getRol());
        Rol rol = rolRepository.findByNombre(nombreRol)
                .orElseThrow(() -> new ResourceNotFoundException("Rol no encontrado: " + request.getRol()));

        Usuario usuario = Usuario.builder()
                .nombre(request.getNombre())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .activo(true)
                .roles(Set.of(rol))
                .build();

        usuario = usuarioRepository.save(usuario);

        UserDetails userDetails = userDetailsService.loadUserByUsername(usuario.getEmail());
        Map<String, Object> claims = buildClaims(usuario);
        String token = jwtUtil.generateToken(userDetails, claims);

        return buildAuthResponse(usuario, token);
    }

    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));

        Usuario usuario = usuarioRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        UserDetails userDetails = userDetailsService.loadUserByUsername(usuario.getEmail());
        Map<String, Object> claims = buildClaims(usuario);
        String token = jwtUtil.generateToken(userDetails, claims);

        return buildAuthResponse(usuario, token);
    }

    private Map<String, Object> buildClaims(Usuario usuario) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", usuario.getId().toString());
        claims.put("nombre", usuario.getNombre());
        claims.put("roles", usuario.getRoles().stream()
                .map(r -> r.getNombre().name())
                .collect(Collectors.toList()));
        return claims;
    }

    private AuthResponse buildAuthResponse(Usuario usuario, String token) {
        return AuthResponse.builder()
                .id(usuario.getId())
                .email(usuario.getEmail())
                .nombre(usuario.getNombre())
                .roles(usuario.getRoles().stream()
                        .map(r -> r.getNombre().name())
                        .collect(Collectors.toSet()))
                .token(token)
                .tokenType("Bearer")
                .build();
    }
}
