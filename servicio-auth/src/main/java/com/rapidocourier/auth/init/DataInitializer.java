package com.rapidocourier.auth.init;

import com.rapidocourier.auth.entity.Rol;
import com.rapidocourier.auth.entity.Usuario;
import com.rapidocourier.auth.repository.RolRepository;
import com.rapidocourier.auth.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final RolRepository rolRepository;
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(RolRepository rolRepository,
                           UsuarioRepository usuarioRepository,
                           PasswordEncoder passwordEncoder) {
        this.rolRepository = rolRepository;
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        crearRoles();
        crearUsuariosIniciales();
    }

    private void crearRoles() {
        for (Rol.NombreRol nombre : Rol.NombreRol.values()) {
            if (rolRepository.findByNombre(nombre).isEmpty()) {
                rolRepository.save(Rol.builder().nombre(nombre).build());
                log.info("Rol creado: {}", nombre);
            }
        }
    }

    private void crearUsuariosIniciales() {
        crearUsuarioSiNoExiste("admin@rapidocourier.pe", "Admin1234", "Administrador", Rol.NombreRol.ADMIN);
        crearUsuarioSiNoExiste("operador@rapidocourier.pe", "Operador1234", "Operador Principal", Rol.NombreRol.OPERADOR);
        crearUsuarioSiNoExiste("cliente@rapidocourier.pe", "Cliente1234", "Cliente Demo", Rol.NombreRol.CLIENTE);
    }

    private void crearUsuarioSiNoExiste(String email, String password, String nombre, Rol.NombreRol rolNombre) {
        if (!usuarioRepository.existsByEmail(email)) {
            Rol rol = rolRepository.findByNombre(rolNombre)
                    .orElseThrow(() -> new RuntimeException("Rol no encontrado: " + rolNombre));
            Usuario usuario = Usuario.builder()
                    .email(email)
                    .password(passwordEncoder.encode(password))
                    .nombre(nombre)
                    .activo(true)
                    .roles(Set.of(rol))
                    .build();
            usuarioRepository.save(usuario);
            log.info("Usuario creado: {} con rol {}", email, rolNombre);
        }
    }
}
