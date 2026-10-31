package com.VetSphere.Backend.modules.clinic.controller;

import com.VetSphere.Backend.exception.BusinessException;
import com.VetSphere.Backend.exception.ResourceNotFoundException;
import com.VetSphere.Backend.modules.clinic.domain.Usuario;
import com.VetSphere.Backend.modules.clinic.repository.UsuarioRepository;
import com.VetSphere.Backend.modules.clinic.service.AuthService;
import com.VetSphere.Backend.security.AuthenticatorTotpService;
import com.VetSphere.Backend.shared.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.security.SecureRandom;

/**
 * Cuenta del usuario autenticado: estado del acceso, cambio de contrasena y
 * datos de contacto.
 */
@Tag(name = "Mi cuenta", description = "Estado de la cuenta y cambio de contrasena del usuario autenticado")
@RestController
@RequestMapping("/api/cuenta")
@RequiredArgsConstructor
public class CuentaController {
    private static final SecureRandom ALEATORIO = new SecureRandom();
    private static final String ALFABETO_CODIGOS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticatorTotpService totp;

    public record CodigoRequest(@NotBlank String codigo) {}
    public record DesactivarAutenticadorRequest(@NotBlank String password, @NotBlank String codigo) {}
    public record PasswordSegundoFactorRequest(@NotBlank String password) {}

    @PostMapping("/autenticador/configurar")
    public ApiResponse<Map<String, Object>> configurarAutenticador() {
        Usuario usuario = usuarioAutenticado();
        if (Boolean.TRUE.equals(usuario.getAutenticadorHabilitado())) throw new BusinessException("El autenticador ya está activo.");
        String secreto = totp.nuevoSecreto();
        usuario.setSecretoAutenticador(secreto);
        usuarioRepository.save(usuario);
        return ApiResponse.ok("Escanea el QR y confirma con el código de tu aplicación.", Map.of("secreto", secreto,
                "qrDataUri", totp.qrDataUri(totp.uri(usuario.getEmail(), secreto))));
    }

    @PostMapping("/autenticador/activar")
    public ApiResponse<Void> activarAutenticador(@Valid @RequestBody CodigoRequest request) {
        Usuario usuario = usuarioAutenticado();
        if (!totp.valido(usuario.getSecretoAutenticador(), request.codigo())) throw new BusinessException("El código de autenticación es incorrecto.");
        usuario.setAutenticadorHabilitado(true);
        usuarioRepository.save(usuario);
        return ApiResponse.ok("Verificación en dos pasos activada.", null);
    }

    @PostMapping("/autenticador/desactivar")
    public ApiResponse<Void> desactivarAutenticador(@RequestBody DesactivarAutenticadorRequest request) {
        Usuario usuario = usuarioAutenticado();
        if (usuario.getPassword() == null || !passwordEncoder.matches(request.password(), usuario.getPassword())
                || !totp.valido(usuario.getSecretoAutenticador(), request.codigo())) throw new BusinessException("La contraseña o el código son incorrectos.");
        usuario.setAutenticadorHabilitado(false);
        usuario.setSecretoAutenticador(null);
        usuarioRepository.save(usuario);
        return ApiResponse.ok("Verificación en dos pasos desactivada.", null);
    }

    @PostMapping("/segundo-factor/correo")
    public ApiResponse<Void> segundoFactorCorreo(@Valid @RequestBody PasswordSegundoFactorRequest request,
                                                  @RequestParam boolean activo) {
        Usuario usuario = usuarioAutenticado();
        validarPassword(request.password(), usuario);
        if (activo && !usuario.correoVerificado()) {
            throw new BusinessException("Verifica tu correo antes de activar este método.");
        }
        usuario.setSegundoFactorCorreoHabilitado(activo);
        usuarioRepository.save(usuario);
        return ApiResponse.ok(activo ? "Verificación por correo activada." : "Verificación por correo desactivada.", null);
    }

    @PostMapping("/segundo-factor/codigos-respaldo")
    public ApiResponse<List<String>> generarCodigosRespaldo(@Valid @RequestBody PasswordSegundoFactorRequest request) {
        Usuario usuario = usuarioAutenticado();
        validarPassword(request.password(), usuario);
        if (!Boolean.TRUE.equals(usuario.getAutenticadorHabilitado())
                && !Boolean.TRUE.equals(usuario.getSegundoFactorCorreoHabilitado())) {
            throw new BusinessException("Activa primero el autenticador o la verificación por correo.");
        }
        List<String> codigos = new ArrayList<>();
        List<String> hashes = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            String codigo = nuevoCodigoRespaldo();
            codigos.add(codigo);
            hashes.add(passwordEncoder.encode(codigo.replace("-", "")));
        }
        usuario.setCodigosRespaldoHash(String.join(",", hashes));
        usuarioRepository.save(usuario);
        return ApiResponse.ok("Guarda estos códigos ahora. Cada uno se puede utilizar una sola vez.", codigos);
    }

    private String nuevoCodigoRespaldo() {
        StringBuilder codigo = new StringBuilder(14);
        for (int i = 0; i < 12; i++) {
            if (i == 4 || i == 8) codigo.append('-');
            codigo.append(ALFABETO_CODIGOS.charAt(ALEATORIO.nextInt(ALFABETO_CODIGOS.length())));
        }
        return codigo.toString();
    }

    private void validarPassword(String password, Usuario usuario) {
        if (usuario.getPassword() == null || !passwordEncoder.matches(password, usuario.getPassword())) {
            throw new BusinessException("La contraseña actual es incorrecta.");
        }
    }

    /** Payload de cambio de contrasena del propio usuario. */
    public record CambioPasswordRequest(
            @NotBlank(message = "La contrasena actual es obligatoria") String passwordActual,
            @NotBlank(message = "La nueva contrasena es obligatoria")
            @Size(min = 8, max = 72, message = "La contrasena debe tener entre 8 y 72 caracteres") String nuevaPassword) {
    }

    public record PerfilRequest(
            @NotBlank(message = "Los nombres son obligatorios") @Size(max = 80) String nombres,
            @NotBlank(message = "Los apellidos son obligatorios") @Size(max = 80) String apellidos,
            @Size(max = 30, message = "El telefono no puede superar los 30 caracteres") String telefono) {
    }

    @GetMapping("/perfil")
    @Operation(summary = "Estado de la cuenta del usuario autenticado")
    public ApiResponse<Map<String, Object>> perfil() {
        Usuario usuario = usuarioAutenticado();
        Map<String, Object> datos = new LinkedHashMap<>();
        datos.put("id", usuario.getId());
        datos.put("nombres", usuario.getNombres());
        datos.put("apellidos", usuario.getApellidos());
        datos.put("email", usuario.getEmail());
        datos.put("telefono", usuario.getTelefono());
        datos.put("activo", usuario.isActivo());
        datos.put("emailVerificado", usuario.correoVerificado());
        datos.put("bloqueado", usuario.bloqueadoAhora());
        datos.put("bloqueadoHasta", usuario.getBloqueadoHasta());
        datos.put("motivoBloqueo", usuario.getMotivoBloqueo());
        datos.put("intentosFallidos", usuario.intentosFallidosEfectivos());
        datos.put("ultimoAcceso", usuario.getUltimoAcceso());
        datos.put("roles", usuario.getRoles().stream().map(r -> r.getNombre()).toList());
        datos.put("autenticadorHabilitado", Boolean.TRUE.equals(usuario.getAutenticadorHabilitado()));
        datos.put("segundoFactorCorreoHabilitado", Boolean.TRUE.equals(usuario.getSegundoFactorCorreoHabilitado()));
        datos.put("codigosRespaldoRestantes", usuario.getCodigosRespaldoHash() == null || usuario.getCodigosRespaldoHash().isBlank()
                ? 0 : usuario.getCodigosRespaldoHash().split(",").length);
        return ApiResponse.ok("Perfil", datos);
    }

    @PutMapping("/perfil")
    @Operation(summary = "Actualiza los datos personales del usuario autenticado")
    public ApiResponse<Map<String, Object>> actualizarPerfil(@Valid @RequestBody PerfilRequest request) {
        Usuario usuario = usuarioAutenticado();
        usuario.setNombres(request.nombres().trim());
        usuario.setApellidos(request.apellidos().trim());
        usuario.setTelefono(request.telefono() == null ? "" : request.telefono().trim());
        usuarioRepository.save(usuario);
        return perfil();
    }

    @PutMapping("/contrasena")
    @Operation(summary = "Cambia la contrasena del usuario autenticado")
    public ApiResponse<Void> cambiarPassword(@RequestBody CambioPasswordRequest request) {
        Usuario usuario = usuarioAutenticado();
        if (usuario.getPassword() == null || !passwordEncoder.matches(request.passwordActual(), usuario.getPassword())) {
            throw new BusinessException("La contrasena actual no es correcta.");
        }
        if (request.passwordActual().equals(request.nuevaPassword())) {
            throw new BusinessException("La nueva contrasena debe ser distinta de la actual.");
        }
        usuario.setPassword(passwordEncoder.encode(request.nuevaPassword()));
        // Tras cambiar la credencial se reinicia el control de intentos y bloqueos.
        usuario.setIntentosFallidos(0);
        usuario.setBloqueadoHasta(null);
        usuario.setMotivoBloqueo(null);
        usuarioRepository.save(usuario);
        return ApiResponse.ok("Contrasena actualizada", null);
    }

    private Usuario usuarioAutenticado() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null) {
            throw new BusinessException("No hay una sesion activa.");
        }
        return usuarioRepository.findByEmail(AuthService.normalizar(authentication.getName()))
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado."));
    }
}
