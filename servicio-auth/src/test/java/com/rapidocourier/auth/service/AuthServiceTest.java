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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

// Nota sobre @Spy: AuthService no tiene componentes auxiliares parcialmente observables
// (e.g., un calculador de tarifa reutilizado entre servicios). La lógica de construcción
// de claims y de respuesta son métodos privados del mismo servicio. Por tanto no aplica @Spy;
// todas las dependencias se mockean completamente con @Mock.
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UsuarioRepository usuarioRepository;
    @Mock private RolRepository rolRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtUtil jwtUtil;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private UserDetailsService userDetailsService;

    @InjectMocks
    private AuthService authService;

    private RegisterRequest registerRequest;
    private LoginRequest loginRequest;
    private Rol rolCliente;
    private Usuario usuario;
    private UserDetails userDetails;

    @BeforeEach
    void setUp() {
        rolCliente = Rol.builder()
                .id(UUID.randomUUID())
                .nombre(Rol.NombreRol.CLIENTE)
                .build();

        usuario = Usuario.builder()
                .id(UUID.randomUUID())
                .email("cliente@test.com")
                .nombre("Cliente Test")
                .password("$2a$10$encoded")
                .activo(true)
                .roles(Set.of(rolCliente))
                .build();

        registerRequest = new RegisterRequest();
        registerRequest.setNombre("Cliente Test");
        registerRequest.setEmail("cliente@test.com");
        registerRequest.setPassword("Password1");
        registerRequest.setRol("CLIENTE");

        loginRequest = new LoginRequest();
        loginRequest.setEmail("cliente@test.com");
        loginRequest.setPassword("Password1");

        userDetails = mock(UserDetails.class);
        // lenient: jwtUtil está mockeado, por lo que getUsername() no siempre se invoca
        lenient().when(userDetails.getUsername()).thenReturn("cliente@test.com");
    }

    @Test
    @DisplayName("Happy Path: registro exitoso devuelve AuthResponse con token")
    void register_exitoso_devuelveToken() {
        when(usuarioRepository.existsByEmail(registerRequest.getEmail())).thenReturn(false);
        when(rolRepository.findByNombre(Rol.NombreRol.CLIENTE)).thenReturn(Optional.of(rolCliente));
        when(passwordEncoder.encode(anyString())).thenReturn("$2a$10$encoded");
        when(usuarioRepository.save(any(Usuario.class))).thenReturn(usuario);
        when(userDetailsService.loadUserByUsername(usuario.getEmail())).thenReturn(userDetails);
        when(jwtUtil.generateToken(any(), any())).thenReturn("jwt-token-test");

        AuthResponse response = authService.register(registerRequest);

        assertThat(response).isNotNull();
        assertThat(response.getEmail()).isEqualTo("cliente@test.com");
        assertThat(response.getToken()).isEqualTo("jwt-token-test");
        assertThat(response.getTokenType()).isEqualTo("Bearer");
        assertThat(response.getRoles()).contains("CLIENTE");
        verify(usuarioRepository, times(1)).save(any(Usuario.class));
    }

    @Test
    @DisplayName("Exception: email duplicado en registro lanza ConflictException")
    void register_emailDuplicado_lanzaConflict() {
        when(usuarioRepository.existsByEmail(registerRequest.getEmail())).thenReturn(true);

        ConflictException ex = assertThrows(ConflictException.class,
                () -> authService.register(registerRequest));

        assertThat(ex.getMessage()).contains("email");
        verify(usuarioRepository, never()).save(any());
        verify(rolRepository, never()).findByNombre(any());
    }

    @Test
    @DisplayName("Exception: rol no encontrado en registro lanza ResourceNotFoundException")
    void register_rolNoExiste_lanzaNotFound() {
        when(usuarioRepository.existsByEmail(registerRequest.getEmail())).thenReturn(false);
        when(rolRepository.findByNombre(Rol.NombreRol.CLIENTE)).thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> authService.register(registerRequest));

        assertThat(ex.getMessage()).contains("CLIENTE");
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    @DisplayName("Happy Path: login exitoso devuelve AuthResponse con token")
    void login_exitoso_devuelveToken() {
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(null);
        when(usuarioRepository.findByEmail(loginRequest.getEmail())).thenReturn(Optional.of(usuario));
        when(userDetailsService.loadUserByUsername(usuario.getEmail())).thenReturn(userDetails);
        when(jwtUtil.generateToken(any(), any())).thenReturn("jwt-login-token");

        AuthResponse response = authService.login(loginRequest);

        assertThat(response).isNotNull();
        assertThat(response.getEmail()).isEqualTo("cliente@test.com");
        assertThat(response.getToken()).isEqualTo("jwt-login-token");
        assertThat(response.getTokenType()).isEqualTo("Bearer");
    }

    @Test
    @DisplayName("Exception: credenciales inválidas en login lanza BadCredentialsException")
    void login_credencialesInvalidas_lanzaException() {
        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        assertThrows(BadCredentialsException.class, () -> authService.login(loginRequest));

        verify(usuarioRepository, never()).findByEmail(anyString());
    }

    @Test
    @DisplayName("Exception: usuario no encontrado tras autenticación lanza ResourceNotFoundException")
    void login_usuarioNoEncontrado_lanzaNotFound() {
        when(authenticationManager.authenticate(any())).thenReturn(null);
        when(usuarioRepository.findByEmail(loginRequest.getEmail())).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> authService.login(loginRequest));

        verify(jwtUtil, never()).generateToken(any(), any());
    }

    @Test
    @DisplayName("Happy Path: registro admin devuelve rol ADMIN en response")
    void register_adminRole_devuelveRolAdmin() {
        Rol rolAdmin = Rol.builder().id(UUID.randomUUID()).nombre(Rol.NombreRol.ADMIN).build();
        Usuario admin = Usuario.builder()
                .id(UUID.randomUUID())
                .email("admin@test.com")
                .nombre("Admin Test")
                .password("$2a$10$encoded")
                .activo(true)
                .roles(Set.of(rolAdmin))
                .build();

        registerRequest.setEmail("admin@test.com");
        registerRequest.setRol("ADMIN");

        when(usuarioRepository.existsByEmail("admin@test.com")).thenReturn(false);
        when(rolRepository.findByNombre(Rol.NombreRol.ADMIN)).thenReturn(Optional.of(rolAdmin));
        when(passwordEncoder.encode(anyString())).thenReturn("$2a$10$encoded");
        when(usuarioRepository.save(any(Usuario.class))).thenReturn(admin);
        when(userDetailsService.loadUserByUsername("admin@test.com")).thenReturn(userDetails);
        when(jwtUtil.generateToken(any(), any())).thenReturn("admin-token");

        AuthResponse response = authService.register(registerRequest);

        assertThat(response.getRoles()).contains("ADMIN");
        assertThat(response.getToken()).isEqualTo("admin-token");
    }
}
