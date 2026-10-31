package com.VetSphere.Backend.modules.clinic.service;

import com.VetSphere.Backend.exception.AccountLockedException;
import com.VetSphere.Backend.exception.BusinessException;
import com.VetSphere.Backend.modules.clinic.dto.CodigoRequest;
import com.VetSphere.Backend.modules.clinic.dto.LoginRequest;
import com.VetSphere.Backend.modules.clinic.dto.RecuperacionPasswordRequest;
import com.VetSphere.Backend.modules.clinic.dto.RegistroRequest;
import com.VetSphere.Backend.modules.clinic.dto.RestablecerPasswordRequest;
import com.VetSphere.Backend.modules.clinic.dto.UsuarioRequest;
import com.VetSphere.Backend.modules.clinic.dto.VerificacionDosPasosRequest;
import com.VetSphere.Backend.modules.clinic.domain.CodigoVerificacion;
import com.VetSphere.Backend.modules.clinic.domain.Usuario;
import com.VetSphere.Backend.modules.clinic.domain.SolicitudRegistro;
import com.VetSphere.Backend.modules.clinic.repository.CodigoVerificacionRepository;
import com.VetSphere.Backend.modules.clinic.repository.RolRepository;
import com.VetSphere.Backend.modules.clinic.repository.UsuarioRepository;
import com.VetSphere.Backend.modules.clinic.repository.SolicitudRegistroRepository;
import com.VetSphere.Backend.shared.response.AuthResponse;
import com.VetSphere.Backend.security.JwtService;
import com.VetSphere.Backend.security.AuthenticatorTotpService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Value;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Autenticacion, verificacion de correo, control de intentos de acceso y
 * administracion del estado de la cuenta (bloqueo / desactivacion).
 *
 * Reglas de seguridad implementadas:
 * <ul>
 *   <li>3 intentos fallidos de acceso bloquean la cuenta durante 3 horas.</li>
 *   <li>Los mensajes de error no revelan si un correo existe o no
 *       (evita enumeracion de cuentas).</li>
 *   <li>Cada codigo por correo tiene un proposito ({@code tipo}); un codigo de
 *       recuperacion no sirve para desactivar la cuenta.</li>
 *   <li>Se limita el numero de codigos emitidos por hora y correo.</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class AuthService {
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    /** Maximo de intentos fallidos consecutivos antes de bloquear la cuenta. */
    public static final int MAX_INTENTOS = 3;
    /** Horas de bloqueo automatico tras agotar los intentos. */
    public static final int HORAS_BLOQUEO = 3;
    /** Maximo de codigos por correo y proposito dentro de la ventana de una hora. */
    private static final int MAX_CODIGOS_POR_HORA = 5;
    /** Ventana de expiracion de los codigos por correo (minutos). */
    private static final int MINUTOS_EXPIRACION = 10;
    private static final String ALFABETO_RESPALDO = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";

    /** Politica de bloqueo configurable desde application.properties. */
    @Value("${app.seguridad.max-intentos:3}")
    private int maxIntentos;

    @Value("${app.seguridad.horas-bloqueo:3}")
    private int horasBloqueo;

    @Value("${app.seguridad.minutos-codigo:10}")
    private int minutosCodigo;

    @Value("${app.seguridad.max-codigos-hora:5}")
    private int maxCodigosPorHora;

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final CodigoVerificacionRepository codigoRepository;
    private final SolicitudRegistroRepository solicitudRegistroRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final CorreoService correoService;
    private final AuditoriaService auditoriaService;
    private final AuthenticatorTotpService totp;

    @Value("${app.registration.admin-code:}")
    private String codigoRegistroPersonal;

    // ------------------------------------------------------------------
    // Inicio de sesion y control de intentos
    // ------------------------------------------------------------------

    @Transactional(noRollbackFor = BusinessException.class)
    public AuthResponse login(LoginRequest request) {
        String email = normalizar(request.email());
        Usuario usuario = usuarioRepository.findByEmail(email).orElse(null);
        if (usuario == null) {
            // Mismo mensaje que una contrasena incorrecta: no se revela si el correo existe.
            auditoriaService.registrar(AuditoriaService.LOGIN_FALLIDO, email, "Usuario", null,
                    "Intento de acceso con correo no registrado");
            throw new BusinessException("Credenciales incorrectas.");
        }
        if (usuario.bloqueadoAhora()) {
            auditoriaService.registrar(AuditoriaService.LOGIN_BLOQUEADO, email, "Usuario", usuario.getId(),
                    "Acceso rechazado: cuenta bloqueada hasta " + usuario.getBloqueadoHasta());
            throw new AccountLockedException(mensajeBloqueo(usuario));
        }
        if (!usuario.isActivo()) {
            auditoriaService.registrar(AuditoriaService.LOGIN_FALLIDO, email, "Usuario", usuario.getId(),
                    "Acceso rechazado: cuenta desactivada");
            throw new BusinessException("La cuenta esta desactivada. Reactivala con un codigo enviado a tu correo.");
        }
        boolean valido = usuario.getPassword() != null
                && passwordEncoder.matches(request.password(), usuario.getPassword());
        if (!valido) {
            registrarIntentoFallido(usuario);
            throw new BusinessException("Credenciales incorrectas.");
        }
        var metodosSegundoFactor = metodosSegundoFactor(usuario);
        if (!metodosSegundoFactor.isEmpty()) {
            if (Boolean.TRUE.equals(usuario.getSegundoFactorCorreoHabilitado())
                    && !Boolean.TRUE.equals(usuario.getAutenticadorHabilitado())) {
                emitirCodigo(email, CodigoVerificacion.SEGUNDO_FACTOR_CORREO);
            }
            return new AuthResponse(null, usuario.getEmail(), usuario.getNombres() + " " + usuario.getApellidos(),
                    usuario.getRoles().stream().map(r -> r.getNombre()).toList(), true, metodosSegundoFactor);
        }
        // Acceso correcto: se reinicia el contador y se levanta el bloqueo automatico vencido.
        usuario.setIntentosFallidos(0);
        usuario.setBloqueadoHasta(null);
        usuario.setMotivoBloqueo(null);
        usuario.setUltimoAcceso(LocalDateTime.now());
        usuarioRepository.save(usuario);
        auditoriaService.registrar(AuditoriaService.LOGIN_EXITOSO, usuario.getEmail(),
                "Usuario", usuario.getId(), "Sesion iniciada correctamente");
        return tokenDe(usuario);
    }

    /**
     * Registra un intento fallido. Al llegar al limite la cuenta queda bloqueada
     * durante {@link #HORAS_BLOQUEO} horas y se notifica al titular por correo.
     */
    private void registrarIntentoFallido(Usuario usuario) {
        int limite = maxIntentos > 0 ? maxIntentos : MAX_INTENTOS;
        int intentos = usuario.intentosFallidosEfectivos() + 1;
        usuario.setIntentosFallidos(intentos);
        String detalle = "Credenciales incorrectas. Intento " + intentos + " de " + limite + ".";
        if (intentos >= limite) {
            int horas = horasBloqueo > 0 ? horasBloqueo : HORAS_BLOQUEO;
            usuario.setIntentosFallidos(0);
            usuario.setBloqueadoHasta(LocalDateTime.now().plusHours(horas));
            usuario.setMotivoBloqueo("Bloqueo automatico por " + limite + " intentos fallidos consecutivos");
            detalle = "Cuenta bloqueada " + horas + " horas por " + limite + " intentos fallidos";
            auditoriaService.registrar(AuditoriaService.LOGIN_BLOQUEADO, usuario.getEmail(),
                    "Usuario", usuario.getId(), detalle);
            correoService.enviarAvisoBloqueo(usuario.getEmail(), horas);
        } else {
            auditoriaService.registrar(AuditoriaService.LOGIN_FALLIDO, usuario.getEmail(),
                    "Usuario", usuario.getId(), detalle);
        }
        usuarioRepository.save(usuario);
    }

    private String mensajeBloqueo(Usuario usuario) {
        LocalDateTime hasta = usuario.getBloqueadoHasta();
        long minutos = Math.max(1, Duration.between(LocalDateTime.now(), hasta).toMinutes());
        long horas = minutos / 60;
        long resto = minutos % 60;
        String ventana = horas > 0
                ? horas + (horas == 1 ? " hora" : " horas") + (resto > 0 ? " y " + resto + " min" : "")
                : resto + " minutos";
        return "La cuenta esta bloqueada por intentos fallidos. Intenta nuevamente en " + ventana + ".";
    }

    // ------------------------------------------------------------------
    // Registro y verificacion de correo
    // ------------------------------------------------------------------

    @Transactional
    public Usuario registrar(RegistroRequest request) {
        String rolSolicitado = request.rolSolicitado() == null || request.rolSolicitado().isBlank()
                ? "ROLE_CLIENTE"
                : request.rolSolicitado().toUpperCase(Locale.ROOT);
        if (!Set.of("ROLE_CLIENTE", "ROLE_VETERINARIO", "ROLE_RECEPCIONISTA").contains(rolSolicitado)) {
            rolSolicitado = "ROLE_CLIENTE";
        }
        String documento = request.documento() == null ? "" : request.documento().trim();
        if ("ROLE_CLIENTE".equals(rolSolicitado) && !documento.matches("[0-9]{10}")) {
            throw new BusinessException("El número de cédula es obligatorio y debe tener 10 dígitos.");
        }
        String email = normalizar(request.email());
        if (usuarioRepository.findByEmail(email).isPresent()) {
            throw new BusinessException("El correo ya esta registrado.");
        }
        if (Set.of("ROLE_VETERINARIO", "ROLE_RECEPCIONISTA").contains(rolSolicitado)) {
            if (!validarCodigoRegistroPersonal(request.codigoAdmin())) {
                throw new BusinessException("Codigo de autorizacion de administrador incorrecto.");
            }
        }
        var rol = rolRepository.findByNombre(rolSolicitado).orElseThrow();

        if (Set.of("ROLE_VETERINARIO", "ROLE_RECEPCIONISTA").contains(rolSolicitado)) {
            solicitudRegistroRepository.save(SolicitudRegistro.builder()
                    .nombres(request.nombres())
                    .apellidos(request.apellidos())
                    .email(email)
                    .telefono(request.telefono())
                    .password(passwordEncoder.encode(request.password()))
                    .rolSolicitado(rolSolicitado)
                    .build());
            return Usuario.builder()
                    .nombres(request.nombres())
                    .apellidos(request.apellidos())
                    .email(email)
                    .telefono(request.telefono())
                    .activo(false)
                    .emailVerificado(false)
                    .roles(new HashSet<>(Set.of(rol)))
                    .build();
        }
        Usuario usuario = usuarioRepository.save(Usuario.builder()
                .nombres(request.nombres())
                .apellidos(request.apellidos())
                .email(email)
                .telefono(request.telefono())
                .documento("ROLE_CLIENTE".equals(rolSolicitado) ? documento : null)
                .password(passwordEncoder.encode(request.password()))
                .activo(false)
                .emailVerificado(false)
                .intentosFallidos(0)
                .roles(new HashSet<>(Set.of(rol)))
                .build());
        emitirCodigo(email, CodigoVerificacion.VERIFICACION_CUENTA);
        return usuario;
    }

    private boolean validarCodigoRegistroPersonal(String codigoRecibido) {
        if (codigoRegistroPersonal == null || codigoRegistroPersonal.isBlank()
                || codigoRecibido == null || codigoRecibido.isBlank()) {
            return false;
        }
        return totp.valido(totp.secretoDesdeClave(codigoRegistroPersonal), codigoRecibido);
    }

    public String codigoRegistroActual() {
        if (codigoRegistroPersonal == null || codigoRegistroPersonal.isBlank()) return "";
        return totp.codigoActual(totp.secretoDesdeClave(codigoRegistroPersonal));
    }

    public int segundosCodigoRegistro() { return totp.segundosRestantes(); }

    public AuthResponse verificarAutenticador(String email, String password, String codigo) {
        return verificarSegundoFactor(email, password, "AUTENTICADOR", codigo);
    }

    @Transactional
    public void solicitarCodigoSegundoFactor(String email, String password) {
        Usuario usuario = usuarioParaSegundoFactor(email, password);
        if (!Boolean.TRUE.equals(usuario.getSegundoFactorCorreoHabilitado())) {
            throw new BusinessException("La verificación por correo no está activada.");
        }
        emitirCodigo(usuario.getEmail(), CodigoVerificacion.SEGUNDO_FACTOR_CORREO);
    }

    public AuthResponse verificarSegundoFactor(String email, String password, String metodo, String codigo) {
        Usuario usuario = usuarioParaSegundoFactor(email, password);
        String factor = metodo == null ? "" : metodo.trim().toUpperCase(Locale.ROOT);
        boolean valido = switch (factor) {
            case "AUTENTICADOR" -> Boolean.TRUE.equals(usuario.getAutenticadorHabilitado())
                    && totp.valido(usuario.getSecretoAutenticador(), codigo);
            case "CORREO" -> Boolean.TRUE.equals(usuario.getSegundoFactorCorreoHabilitado())
                    && validarCodigoCorreoFactor(usuario.getEmail(), codigo);
            case "RESPALDO" -> consumirCodigoRespaldo(usuario, codigo);
            default -> false;
        };
        if (!valido) {
            registrarIntentoFallido(usuario);
            throw new BusinessException("Código de verificación incorrecto.");
        }
        return completarInicioSesion(usuario, "Inicio de sesión con segundo factor " + factor);
    }

    private Usuario usuarioParaSegundoFactor(String email, String password) {
        Usuario usuario = usuarioRepository.findByEmail(normalizar(email)).orElseThrow(() -> new BusinessException("Credenciales incorrectas."));
        if (!usuario.isActivo() || usuario.bloqueadoAhora() || usuario.getPassword() == null
                || !passwordEncoder.matches(password, usuario.getPassword()) || metodosSegundoFactor(usuario).isEmpty()) {
            throw new BusinessException("Credenciales incorrectas.");
        }
        return usuario;
    }

    private boolean validarCodigoCorreoFactor(String email, String codigo) {
        if (codigo == null || !codigo.matches("\\d{6}")) return false;
        CodigoVerificacion registro = codigoRepository
                .findTopByEmailAndTipoAndCodigoAndUsadoFalseOrderByIdDesc(
                        email, CodigoVerificacion.SEGUNDO_FACTOR_CORREO, codigo)
                .orElse(null);
        if (registro == null || registro.getExpiraEn() == null || registro.getExpiraEn().isBefore(LocalDateTime.now())) {
            return false;
        }
        registro.setUsado(true);
        codigoRepository.save(registro);
        return true;
    }

    private boolean consumirCodigoRespaldo(Usuario usuario, String codigo) {
        if (codigo == null || usuario.getCodigosRespaldoHash() == null) return false;
        String normalizado = codigo.replace("-", "").trim().toUpperCase(Locale.ROOT);
        var hashes = new java.util.ArrayList<>(java.util.Arrays.asList(usuario.getCodigosRespaldoHash().split(",")));
        int indice = -1;
        for (int i = 0; i < hashes.size(); i++) {
            if (passwordEncoder.matches(normalizado, hashes.get(i))) { indice = i; break; }
        }
        if (indice < 0) return false;
        hashes.remove(indice);
        usuario.setCodigosRespaldoHash(hashes.isEmpty() ? null : String.join(",", hashes));
        usuarioRepository.save(usuario);
        return true;
    }

    private java.util.List<String> metodosSegundoFactor(Usuario usuario) {
        var metodos = new java.util.ArrayList<String>();
        if (Boolean.TRUE.equals(usuario.getAutenticadorHabilitado())) metodos.add("AUTENTICADOR");
        if (Boolean.TRUE.equals(usuario.getSegundoFactorCorreoHabilitado())) metodos.add("CORREO");
        if (usuario.getCodigosRespaldoHash() != null && !usuario.getCodigosRespaldoHash().isBlank()) metodos.add("RESPALDO");
        return metodos;
    }

    private AuthResponse completarInicioSesion(Usuario usuario, String detalle) {
        usuario.setIntentosFallidos(0);
        usuario.setBloqueadoHasta(null);
        usuario.setMotivoBloqueo(null);
        usuario.setUltimoAcceso(LocalDateTime.now());
        usuarioRepository.save(usuario);
        auditoriaService.registrar(AuditoriaService.LOGIN_EXITOSO, usuario.getEmail(), "Usuario", usuario.getId(), detalle);
        return tokenDe(usuario);
    }

    /** Reenvia el codigo de verificacion de cuenta (2FA de registro). */
    @Transactional
    public void reenviarCodigo(String email) {
        String normalizado = normalizar(email);
        Usuario usuario = usuarioRepository.findByEmail(normalizado).orElse(null);
        if (usuario == null) {
            // No se revela si el correo existe.
            return;
        }
        if (usuario.correoVerificado()) {
            throw new BusinessException("La cuenta ya esta verificada. Intenta iniciar sesion.");
        }
        emitirCodigo(normalizado, CodigoVerificacion.VERIFICACION_CUENTA);
    }

    // ------------------------------------------------------------------
    // Recuperacion de contrasena
    // ------------------------------------------------------------------

    @Transactional
    public void solicitarRecuperacion(RecuperacionPasswordRequest request) {
        String email = normalizar(request.email());
        Usuario usuario = usuarioRepository.findByEmail(email).orElse(null);
        if (usuario == null) {
            throw new BusinessException("No encontramos una cuenta registrada con ese correo.");
        }
        emitirCodigo(email, CodigoVerificacion.RECUPERACION);
    }

    @Transactional
    public void restablecerPassword(RestablecerPasswordRequest request) {
        String email = normalizar(request.email());
        CodigoVerificacion codigo = codigoRepository
                .findTopByEmailAndTipoAndCodigoAndUsadoFalseOrderByIdDesc(email,
                        CodigoVerificacion.RECUPERACION, request.codigo())
                .orElseThrow(() -> new BusinessException("Codigo invalido."));
        if (codigo.getExpiraEn().isBefore(LocalDateTime.now())) {
            throw new BusinessException("El codigo expiro. Solicita uno nuevo.");
        }
        Usuario usuario = usuarioRepository.findByEmail(email).orElseThrow();
        usuario.setPassword(passwordEncoder.encode(request.nuevaPassword()));
        // Tras restablecer la credencial se reinicia el control de intentos.
        usuario.setIntentosFallidos(0);
        usuario.setBloqueadoHasta(null);
        usuario.setMotivoBloqueo(null);
        usuarioRepository.save(usuario);
        codigo.setUsado(true);
        codigoRepository.save(codigo);
        auditoriaService.registrar(AuditoriaService.PASSWORD_RESTABLECIDA, email, "Usuario", usuario.getId(),
                "Contrasena restablecida con codigo de correo");
    }

    // ------------------------------------------------------------------
    // Verificacion en dos pasos
    // ------------------------------------------------------------------

    @Transactional
    public AuthResponse verificarDosPasos(VerificacionDosPasosRequest request) {
        String email = normalizar(request.email());
        CodigoVerificacion codigo = codigoRepository
                .findTopByEmailAndTipoAndCodigoAndUsadoFalseOrderByIdDesc(email,
                        CodigoVerificacion.VERIFICACION_CUENTA, request.codigo())
                .orElseThrow(() -> new BusinessException("Codigo invalido."));
        if (codigo.getExpiraEn().isBefore(LocalDateTime.now())) {
            throw new BusinessException("El codigo expiro. Solicita uno nuevo.");
        }
        codigo.setUsado(true);
        codigoRepository.save(codigo);
        Usuario usuario = usuarioRepository.findByEmail(email).orElseThrow();
        usuario.setEmailVerificado(true);
        usuario.setActivo(true);
        usuario.setIntentosFallidos(0);
        usuarioRepository.save(usuario);
        auditoriaService.registrar(AuditoriaService.EMAIL_VERIFICADO, email, "Usuario", usuario.getId(),
                "Correo verificado y cuenta activada");
        return tokenDe(usuario);
    }

    // ------------------------------------------------------------------
    // Baja y reactivacion de cuenta initiated por el propio titular
    // ------------------------------------------------------------------

    /**
     * Paso 1 de la baja: envia un codigo al correo del titular. Siempre responde
     * de forma neutra para no revelar si el correo esta registrado.
     */
    @Transactional
    public void solicitarDesactivacion(RecuperacionPasswordRequest request) {
        String email = normalizar(request.email());
        Usuario usuario = usuarioRepository.findByEmail(email).orElse(null);
        if (usuario == null || !usuario.isActivo()) {
            return;
        }
        emitirCodigo(email, CodigoVerificacion.DESACTIVACION);
    }

    /** Paso 2 de la baja: valida el codigo recibido por correo y desactiva la cuenta. */
    @Transactional
    public void confirmarDesactivacion(CodigoRequest request) {
        String email = normalizar(request.email());
        CodigoVerificacion codigo = codigoRepository
                .findTopByEmailAndTipoAndCodigoAndUsadoFalseOrderByIdDesc(email,
                        CodigoVerificacion.DESACTIVACION, request.codigo())
                .orElseThrow(() -> new BusinessException("Codigo invalido."));
        if (codigo.getExpiraEn().isBefore(LocalDateTime.now())) {
            throw new BusinessException("El codigo expiro. Solicita uno nuevo.");
        }
        Usuario usuario = usuarioRepository.findByEmail(email).orElseThrow();
        usuario.setActivo(false);
        usuario.setDesactivadoEn(LocalDateTime.now());
        usuario.setIntentosFallidos(0);
        usuario.setBloqueadoHasta(null);
        usuario.setMotivoBloqueo("Cuenta desactivada por su titular");
        usuarioRepository.save(usuario);
        codigo.setUsado(true);
        codigoRepository.save(codigo);
        auditoriaService.registrar(AuditoriaService.CUENTA_DESACTIVADA, email, "Usuario", usuario.getId(),
                "Cuenta desactivada por el titular con verificacion de correo");
        correoService.enviarAvisoAdministrativo(email, "Desactivacion solicitada por el titular");
    }

    /** Paso 1 de la reactivacion: envia un codigo al correo del titular. */
    @Transactional
    public void solicitarReactivacion(RecuperacionPasswordRequest request) {
        String email = normalizar(request.email());
        Usuario usuario = usuarioRepository.findByEmail(email).orElse(null);
        if (usuario == null || usuario.isActivo()) {
            return;
        }
        emitirCodigo(email, CodigoVerificacion.REACTIVACION);
    }

    /** Paso 2 de la reactivacion: valida el codigo y vuelve a activar la cuenta. */
    @Transactional
    public void confirmarReactivacion(CodigoRequest request) {
        String email = normalizar(request.email());
        CodigoVerificacion codigo = codigoRepository
                .findTopByEmailAndTipoAndCodigoAndUsadoFalseOrderByIdDesc(email,
                        CodigoVerificacion.REACTIVACION, request.codigo())
                .orElseThrow(() -> new BusinessException("Codigo invalido."));
        if (codigo.getExpiraEn().isBefore(LocalDateTime.now())) {
            throw new BusinessException("El codigo expiro. Solicita uno nuevo.");
        }
        Usuario usuario = usuarioRepository.findByEmail(email).orElseThrow();
        usuario.setActivo(true);
        usuario.setDesactivadoEn(null);
        usuario.setIntentosFallidos(0);
        usuario.setBloqueadoHasta(null);
        usuario.setMotivoBloqueo(null);
        usuarioRepository.save(usuario);
        codigo.setUsado(true);
        codigoRepository.save(codigo);
        auditoriaService.registrar(AuditoriaService.CUENTA_REACTIVADA, email, "Usuario", usuario.getId(),
                "Cuenta reactivada por el titular con verificacion de correo");
    }

    // ------------------------------------------------------------------
    // Utilidades
    // ------------------------------------------------------------------

    /**
     * Emite un codigo de un solo uso para el proposito indicado. Invalida los
     * codigos previos del mismo proposito y limita la cantidad por hora.
     */
    private void emitirCodigo(String email, String tipo) {
        LocalDateTime desde = LocalDateTime.now().minusHours(1);
        long emitidos = codigoRepository.countByEmailAndTipoAndUsadoFalseAndExpiraEnAfter(email, tipo, desde);
        int limite = maxCodigosPorHora > 0 ? maxCodigosPorHora : MAX_CODIGOS_POR_HORA;
        if (emitidos >= limite) {
            throw new BusinessException("Se alcanzo el limite de solicitudes por hora. Intenta mas tarde.");
        }
        codigoRepository.invalidar(email, tipo);
        String codigo = String.valueOf(100000 + SECURE_RANDOM.nextInt(900000));
        int minutos = minutosCodigo > 0 ? minutosCodigo : MINUTOS_EXPIRACION;
        codigoRepository.save(CodigoVerificacion.builder()
                .email(email)
                .codigo(codigo)
                .tipo(tipo)
                .expiraEn(LocalDateTime.now().plusMinutes(minutos))
                .usado(false)
                .build());
        switch (tipo) {
            case CodigoVerificacion.RECUPERACION -> correoService.enviarCodigoRecuperacion(email, codigo);
            case CodigoVerificacion.DESACTIVACION -> correoService.enviarCodigoDesactivacion(email, codigo);
            case CodigoVerificacion.REACTIVACION -> correoService.enviarCodigoReactivacion(email, codigo);
            case CodigoVerificacion.SEGUNDO_FACTOR_CORREO -> correoService.enviarCodigoSegundoFactor(email, codigo);
            default -> correoService.enviarCodigoDosPasos(email, codigo);
        }
    }

    private AuthResponse tokenDe(Usuario usuario) {
        return new AuthResponse(
                jwtService.generarToken(usuario),
                usuario.getEmail(),
                usuario.getNombres() + " " + usuario.getApellidos(),
                usuario.getRoles().stream().map(r -> r.getNombre()).toList());
    }

    /** Normaliza el correo: minusculas y sin espacios (clave de busqueda consistente). */
    public static String normalizar(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }

    /** Formatea un instante para los mensajes al usuario. */
    public static String formatear(LocalDateTime instante) {
        return instante == null ? "sin fecha" : FORMATO_FECHA.format(instante);
    }
}
