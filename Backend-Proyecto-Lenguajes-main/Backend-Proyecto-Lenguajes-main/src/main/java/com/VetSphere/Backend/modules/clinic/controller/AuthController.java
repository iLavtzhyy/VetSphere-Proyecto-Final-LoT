package com.VetSphere.Backend.modules.clinic.controller;

import com.VetSphere.Backend.modules.clinic.dto.CodigoRequest;
import com.VetSphere.Backend.modules.clinic.dto.LoginRequest;
import com.VetSphere.Backend.modules.clinic.dto.RecuperacionPasswordRequest;
import com.VetSphere.Backend.modules.clinic.dto.RegistroRequest;
import com.VetSphere.Backend.modules.clinic.dto.RestablecerPasswordRequest;
import com.VetSphere.Backend.modules.clinic.dto.VerificacionDosPasosRequest;
import com.VetSphere.Backend.modules.clinic.domain.Usuario;
import com.VetSphere.Backend.shared.response.ApiResponse;
import com.VetSphere.Backend.shared.response.AuthResponse;
import com.VetSphere.Backend.modules.clinic.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Autenticacion", description = "Login, registro, verificacion por correo, recuperacion y estado de la cuenta")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

    public record CodigoAutenticadorRequest(String email, String password, String codigo) {}
    public record CodigoSegundoFactorRequest(String email, String password, String metodo, String codigo) {}
    public record SolicitarCodigoSegundoFactorRequest(String email, String password) {}

    @PostMapping("/verificar-autenticador")
    public ApiResponse<AuthResponse> verificarAutenticador(@RequestBody CodigoAutenticadorRequest request) {
        return ApiResponse.ok("Sesion iniciada", authService.verificarAutenticador(request.email(), request.password(), request.codigo()));
    }

    @PostMapping("/solicitar-codigo-segundo-factor")
    public ApiResponse<Void> solicitarCodigoSegundoFactor(@RequestBody SolicitarCodigoSegundoFactorRequest request) {
        authService.solicitarCodigoSegundoFactor(request.email(), request.password());
        return ApiResponse.ok("Si tus credenciales son válidas, enviamos un código al correo de tu cuenta.", null);
    }

    @PostMapping("/verificar-segundo-factor")
    public ApiResponse<AuthResponse> verificarSegundoFactor(@RequestBody CodigoSegundoFactorRequest request) {
        return ApiResponse.ok("Sesión iniciada", authService.verificarSegundoFactor(
                request.email(), request.password(), request.metodo(), request.codigo()));
    }

    @PostMapping("/login")
    @Operation(summary = "Inicia sesion y devuelve el token JWT (423 si la cuenta esta bloqueada)")
    public ApiResponse<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.ok("Sesion iniciada", authService.login(request));
    }

    @PostMapping("/registro")
    @Operation(summary = "Registra un usuario (queda pendiente hasta verificar el codigo por correo)")
    public ApiResponse<Usuario> registro(@Valid @RequestBody RegistroRequest request) {
        return ApiResponse.ok("Usuario registrado. Revisa el correo para verificar la cuenta.", authService.registrar(request));
    }

    @PostMapping("/reenviar-codigo")
    @Operation(summary = "Reenvia el codigo de verificacion de la cuenta")
    public ApiResponse<Void> reenviarCodigo(@Valid @RequestBody RecuperacionPasswordRequest request) {
        authService.reenviarCodigo(request.email());
        return ApiResponse.ok("Si la cuenta existe y aun no esta verificada, enviamos un nuevo codigo.", null);
    }

    @PostMapping("/solicitar-recuperacion")
    @Operation(summary = "Solicita la recuperacion de la contrasena por correo")
    public ApiResponse<Void> solicitarRecuperacion(@Valid @RequestBody RecuperacionPasswordRequest request) {
        authService.solicitarRecuperacion(request);
        return ApiResponse.ok("Si la cuenta existe, enviamos un codigo de recuperacion.", null);
    }

    @PostMapping("/restablecer-password")
    @Operation(summary = "Restablece la contrasena con el codigo recibido")
    public ApiResponse<Void> restablecerPassword(@Valid @RequestBody RestablecerPasswordRequest request) {
        authService.restablecerPassword(request);
        return ApiResponse.ok("Contrasena actualizada", null);
    }

    @PostMapping("/verificar-2fa")
    @Operation(summary = "Verifica el codigo por correo, activa la cuenta y devuelve el token")
    public ApiResponse<AuthResponse> verificar(@Valid @RequestBody VerificacionDosPasosRequest request) {
        return ApiResponse.ok("Cuenta verificada", authService.verificarDosPasos(request));
    }

    @PostMapping("/desactivar-cuenta")
    @Operation(summary = "Paso 1 de la baja de cuenta: envia un codigo de confirmacion al correo")
    public ApiResponse<Void> solicitarDesactivacion(@Valid @RequestBody RecuperacionPasswordRequest request) {
        authService.solicitarDesactivacion(request);
        return ApiResponse.ok("Si la cuenta existe, enviamos un codigo de confirmacion al correo.", null);
    }

    @PostMapping("/confirmar-desactivacion")
    @Operation(summary = "Paso 2 de la baja de cuenta: desactiva el acceso con el codigo recibido")
    public ApiResponse<Void> confirmarDesactivacion(@Valid @RequestBody CodigoRequest request) {
        authService.confirmarDesactivacion(request);
        return ApiResponse.ok("Cuenta desactivada. Conserva el codigo por si necesitas reactivarla.", null);
    }

    @PostMapping("/reactivar-cuenta")
    @Operation(summary = "Paso 1 de la reactivacion: envia un codigo de confirmacion al correo")
    public ApiResponse<Void> solicitarReactivacion(@Valid @RequestBody RecuperacionPasswordRequest request) {
        authService.solicitarReactivacion(request);
        return ApiResponse.ok("Si la cuenta existe, enviamos un codigo de reactivacion.", null);
    }

    @PostMapping("/confirmar-reactivacion")
    @Operation(summary = "Paso 2 de la reactivacion: vuelve a activar el acceso con el codigo recibido")
    public ApiResponse<Void> confirmarReactivacion(@Valid @RequestBody CodigoRequest request) {
        authService.confirmarReactivacion(request);
        return ApiResponse.ok("Cuenta reactivada. Ya puedes iniciar sesion.", null);
    }
}
