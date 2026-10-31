package com.VetSphere.Backend.modules.clinic.service;

import com.VetSphere.Backend.exception.AccountLockedException;
import com.VetSphere.Backend.exception.BusinessException;
import com.VetSphere.Backend.modules.clinic.domain.CodigoVerificacion;
import com.VetSphere.Backend.modules.clinic.domain.Usuario;
import com.VetSphere.Backend.modules.clinic.dto.CodigoRequest;
import com.VetSphere.Backend.modules.clinic.dto.LoginRequest;
import com.VetSphere.Backend.modules.clinic.dto.RecuperacionPasswordRequest;
import com.VetSphere.Backend.modules.clinic.repository.CodigoVerificacionRepository;
import com.VetSphere.Backend.modules.clinic.repository.RolRepository;
import com.VetSphere.Backend.modules.clinic.repository.SolicitudRegistroRepository;
import com.VetSphere.Backend.modules.clinic.repository.UsuarioRepository;
import com.VetSphere.Backend.security.JwtService;
import com.VetSphere.Backend.security.AuthenticatorTotpService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pruebas del control de acceso de la cuenta:
 * bloqueo por intentos fallidos, mensajes neutros (sin enumeracion de correos)
 * y desactivacion/reactivacion de la cuenta por el propio titular.
 */
class AuthServiceTest {

    private static final String EMAIL = "ana@vetsphere.com";
    private static final String CORRECTA = "Clave#Segura1";

    private UsuarioRepository usuarioRepository;
    private CodigoVerificacionRepository codigoRepository;
    private PasswordEncoder passwordEncoder;
    private CorreoService correoService;
    private JwtService jwtService;
    private AuthService authService;

    @BeforeEach
    void configurar() {
        usuarioRepository = mock(UsuarioRepository.class);
        codigoRepository = mock(CodigoVerificacionRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);
        correoService = mock(CorreoService.class);
        jwtService = mock(JwtService.class);

        when(passwordEncoder.matches(anyString(), anyString())).thenAnswer(invoc ->
                CORRECTA.equals(invoc.getArgument(1)) && CORRECTA.equals(invoc.getArgument(0)));
        when(jwtService.generarToken(any(Usuario.class))).thenReturn("token-de-prueba");

        authService = new AuthService(usuarioRepository, mock(RolRepository.class), codigoRepository,
                mock(SolicitudRegistroRepository.class), passwordEncoder, jwtService,
                correoService, mock(AuditoriaService.class), mock(AuthenticatorTotpService.class));
    }

    private Usuario usuarioActivo() {
        return Usuario.builder()
                .id(1L)
                .nombres("Ana")
                .apellidos("Molina")
                .email(EMAIL)
                .password(CORRECTA)
                .activo(true)
                .emailVerificado(true)
                .intentosFallidos(0)
                .build();
    }

    @Test
    void correoNoRegistradoYContrasenaIncorrectaDanElMismoMensaje() {
        when(usuarioRepository.findByEmail("nadie@vetsphere.com")).thenReturn(Optional.empty());
        when(usuarioRepository.findByEmail(EMAIL)).thenReturn(Optional.of(usuarioActivo()));

        BusinessException correoInexistente = assertThrows(BusinessException.class,
                () -> authService.login(new LoginRequest("nadie@vetsphere.com", "mala")));
        BusinessException contrasenaIncorrecta = assertThrows(BusinessException.class,
                () -> authService.login(new LoginRequest(EMAIL, "mala")));

        // El mensaje es identico: no permite saber si un correo esta registrado.
        assertEquals(correoInexistente.getMessage(), contrasenaIncorrecta.getMessage());
        assertEquals("Credenciales incorrectas.", contrasenaIncorrecta.getMessage());
    }

    @Test
    void tresIntentosFallidosBloqueanLaCuentaDuranteTresHoras() {
        Usuario usuario = usuarioActivo();
        when(usuarioRepository.findByEmail(EMAIL)).thenReturn(Optional.of(usuario));

        for (int intento = 1; intento <= 2; intento++) {
            assertThrows(BusinessException.class,
                    () -> authService.login(new LoginRequest(EMAIL, "mala")));
            assertFalse(usuario.bloqueadoAhora(), "No debe bloquearse antes del tercer intento.");
        }

        assertThrows(BusinessException.class,
                () -> authService.login(new LoginRequest(EMAIL, "mala")));

        assertNotNull(usuario.getBloqueadoHasta());
        long horas = Duration.between(LocalDateTime.now(), usuario.getBloqueadoHasta()).toMinutes() / 60;
        assertTrue(horas >= 2 && horas <= 3, "El bloqueo debe durar 3 horas, fue: " + horas);
        assertNotNull(usuario.getMotivoBloqueo());
        // Se avisa al titular por correo.
        verify(correoService, times(1)).enviarAvisoBloqueo(eq(EMAIL), eq(3));
    }

    @Test
    void unaCuentaBloqueadaDevuelveAccountLockedException() {
        Usuario usuario = usuarioActivo();
        usuario.setBloqueadoHasta(LocalDateTime.now().plusHours(2));
        when(usuarioRepository.findByEmail(EMAIL)).thenReturn(Optional.of(usuario));

        AccountLockedException excepcion = assertThrows(AccountLockedException.class,
                () -> authService.login(new LoginRequest(EMAIL, CORRECTA)));

        assertTrue(excepcion.getMessage().contains("bloqueada"));
        // Ni siquiera se comprueba la contrasena cuando la cuenta esta bloqueada.
        verify(usuarioRepository, never()).save(any(Usuario.class));
    }

    @Test
    void unAccesoCorrectoReiniciaElContadorDeIntentos() {
        Usuario usuario = usuarioActivo();
        usuario.setIntentosFallidos(2);
        usuario.setBloqueadoHasta(LocalDateTime.now().minusMinutes(5));
        when(usuarioRepository.findByEmail(EMAIL)).thenReturn(Optional.of(usuario));

        assertNotNull(authService.login(new LoginRequest(EMAIL, CORRECTA)));

        assertEquals(0, usuario.intentosFallidosEfectivos());
        assertNull(usuario.getBloqueadoHasta());
        assertNotNull(usuario.getUltimoAcceso());
    }

    @Test
    void unaCuentaDesactivadaNoPuedeIniciarSesion() {
        Usuario usuario = usuarioActivo();
        usuario.setActivo(false);
        when(usuarioRepository.findByEmail(EMAIL)).thenReturn(Optional.of(usuario));

        BusinessException excepcion = assertThrows(BusinessException.class,
                () -> authService.login(new LoginRequest(EMAIL, CORRECTA)));
        assertTrue(excepcion.getMessage().toLowerCase().contains("desactivada"));
    }

    @Test
    void laDesactivacionExigeUnCodigoDeSeisDigitos() {
        Usuario usuario = usuarioActivo();
        when(usuarioRepository.findByEmail(EMAIL)).thenReturn(Optional.of(usuario));
        CodigoVerificacion codigo = CodigoVerificacion.builder()
                .id(1L)
                .email(EMAIL)
                .codigo("123456")
                .tipo(CodigoVerificacion.DESACTIVACION)
                .expiraEn(LocalDateTime.now().plusMinutes(10))
                .usado(false)
                .build();
        when(codigoRepository.findTopByEmailAndTipoAndCodigoAndUsadoFalseOrderByIdDesc(
                EMAIL, CodigoVerificacion.DESACTIVACION, "123456"))
                .thenReturn(Optional.of(codigo));

        authService.confirmarDesactivacion(new CodigoRequest(EMAIL, "123456"));

        assertFalse(usuario.isActivo());
        assertNotNull(usuario.getDesactivadoEn());
        assertTrue(codigo.isUsado());
    }

    @Test
    void unCodigoDeRecuperacionNoSirveParaDesactivarLaCuenta() {
        when(codigoRepository.findTopByEmailAndTipoAndCodigoAndUsadoFalseOrderByIdDesc(
                EMAIL, CodigoVerificacion.DESACTIVACION, "123456"))
                .thenReturn(Optional.empty());

        assertThrows(BusinessException.class,
                () -> authService.confirmarDesactivacion(new CodigoRequest(EMAIL, "123456")));
    }

    @Test
    void unCodigoExpiradoNoDesactivaLaCuenta() {
        Usuario usuario = usuarioActivo();
        when(usuarioRepository.findByEmail(EMAIL)).thenReturn(Optional.of(usuario));
        CodigoVerificacion codigo = CodigoVerificacion.builder()
                .id(1L)
                .email(EMAIL)
                .codigo("123456")
                .tipo(CodigoVerificacion.DESACTIVACION)
                .expiraEn(LocalDateTime.now().minusMinutes(1))
                .usado(false)
                .build();
        when(codigoRepository.findTopByEmailAndTipoAndCodigoAndUsadoFalseOrderByIdDesc(
                EMAIL, CodigoVerificacion.DESACTIVACION, "123456"))
                .thenReturn(Optional.of(codigo));

        assertThrows(BusinessException.class,
                () -> authService.confirmarDesactivacion(new CodigoRequest(EMAIL, "123456")));
        assertTrue(usuario.isActivo());
    }

    @Test
    void laSolicitudDeDesactivacionRespondeIgualSiElCorreoNoExiste() {
        when(usuarioRepository.findByEmail("nadie@vetsphere.com")).thenReturn(Optional.empty());
        // No debe lanzar: la respuesta es neutra para no revelar que el correo existe.
        authService.solicitarDesactivacion(new RecuperacionPasswordRequest("nadie@vetsphere.com"));
        verify(codigoRepository, never()).save(any(CodigoVerificacion.class));
    }
}
