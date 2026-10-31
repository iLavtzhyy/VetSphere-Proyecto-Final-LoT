package com.VetSphere.Backend.security;

import com.VetSphere.Backend.modules.clinic.domain.Rol;
import com.VetSphere.Backend.modules.clinic.domain.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas unitarias del servicio JWT: generacion y extraccion del token,
 * sin levantar el contexto de Spring.
 */
class JwtServiceTest {

    private static final String SECRETO = "clave-secreta-de-pruebas-0123456789abcdef0123456789abcdef";

    private JwtService jwtService;

    @BeforeEach
    void configurar() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secret", SECRETO);
        ReflectionTestUtils.setField(jwtService, "expirationMs", 1800000L);
    }

    @Test
    void generaTokenYExtraeElEmailDelUsuario() {
        Usuario usuario = Usuario.builder()
                .id(7L)
                .email("admin1@vetsphere.com")
                .nombres("Admin")
                .apellidos("VetSphere")
                .roles(Set.of(Rol.builder().nombre("ROLE_ADMIN").build()))
                .build();

        String token = jwtService.generarToken(usuario);

        assertNotNull(token);
        assertFalse(token.isBlank());
        assertEquals("admin1@vetsphere.com", jwtService.extraerEmail(token));
    }

    @Test
    void tokenFalsoLanzaExcepcionDeFirma() {
        assertThrows(Exception.class, () -> jwtService.extraerEmail("token.invalido.aqui"));
    }
}
