package com.VetSphere.Backend.modules.clinic.controller;

import com.VetSphere.Backend.modules.clinic.dto.BloqueoUsuarioRequest;
import com.VetSphere.Backend.modules.clinic.dto.EstadoUsuarioRequest;
import com.VetSphere.Backend.modules.clinic.dto.UsuarioRequest;
import com.VetSphere.Backend.modules.clinic.dto.VeterinarioPublico;
import com.VetSphere.Backend.modules.clinic.domain.Usuario;
import com.VetSphere.Backend.shared.response.ApiResponse;
import com.VetSphere.Backend.modules.clinic.service.UsuarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Tag(name = "Usuarios", description = "Gestion de usuarios, bloqueo de cuentas y aprobacion de solicitudes")
@RestController
@RequiredArgsConstructor
public class UsuarioController {
    private final UsuarioService usuarioService;

    @GetMapping("/api/admin/usuarios")
    public ApiResponse<List<Usuario>> listar() {
        return ApiResponse.ok("Usuarios", usuarioService.listar());
    }

    @PostMapping("/api/admin/usuarios")
    public ApiResponse<Usuario> crear(@Valid @RequestBody UsuarioRequest request) {
        return ApiResponse.ok("Usuario creado", usuarioService.crear(request));
    }

    @PutMapping("/api/admin/usuarios/{id}")
    public ApiResponse<Usuario> actualizar(@PathVariable Long id, @Valid @RequestBody UsuarioRequest request) {
        return ApiResponse.ok("Usuario actualizado", usuarioService.actualizar(id, request));
    }

    @GetMapping("/api/admin/usuarios/solicitudes-pendientes")
    public ApiResponse<List<Usuario>> solicitudesPendientes() {
        return ApiResponse.ok("Solicitudes pendientes", usuarioService.solicitudesPendientes());
    }

    @PutMapping("/api/admin/usuarios/{id}/aprobar")
    public ApiResponse<Usuario> aprobar(@PathVariable Long id) {
        return ApiResponse.ok("Usuario aprobado", usuarioService.aprobar(id));
    }

    @PutMapping("/api/admin/usuarios/{id}/rechazar")
    public ApiResponse<Void> rechazar(@PathVariable Long id) {
        usuarioService.rechazar(id);
        return ApiResponse.ok("Solicitud rechazada", null);
    }

    @PutMapping("/api/admin/usuarios/{id}/roles")
    public ApiResponse<Usuario> actualizarRoles(@PathVariable Long id, @RequestBody Map<String, Set<String>> request) {
        return ApiResponse.ok("Roles actualizados", usuarioService.actualizarRoles(id, request.getOrDefault("roles", Set.of("ROLE_CLIENTE"))));
    }

    /**
     * Bloquea el acceso de un usuario de forma manual. Las credenciales ya
     * emitidas quedan invalidas porque el filtro JWT valida el bloqueo.
     */
    @PutMapping("/api/admin/usuarios/{id}/bloquear")
    @Operation(summary = "Bloquea la cuenta de un usuario (por defecto 24 horas)")
    public ApiResponse<Usuario> bloquear(@PathVariable Long id, @RequestBody BloqueoUsuarioRequest request) {
        return ApiResponse.ok("Cuenta bloqueada", usuarioService.bloquear(id,
                request == null ? null : request.horas(),
                request == null ? null : request.motivo()));
    }

    @PutMapping("/api/admin/usuarios/{id}/desbloquear")
    @Operation(summary = "Levanta el bloqueo y reinicia el contador de intentos")
    public ApiResponse<Usuario> desbloquear(@PathVariable Long id) {
        return ApiResponse.ok("Cuenta desbloqueada", usuarioService.desbloquear(id));
    }

    @PutMapping("/api/admin/usuarios/{id}/desactivar")
    @Operation(summary = "Desactiva la cuenta: el usuario no puede iniciar sesion")
    public ApiResponse<Usuario> desactivar(@PathVariable Long id, @RequestBody(required = false) EstadoUsuarioRequest request) {
        return ApiResponse.ok("Cuenta desactivada", usuarioService.desactivar(id,
                request == null ? null : request.motivo()));
    }

    @PutMapping("/api/admin/usuarios/{id}/reactivar")
    @Operation(summary = "Reactiva una cuenta desactivada")
    public ApiResponse<Usuario> reactivar(@PathVariable Long id, @RequestBody(required = false) EstadoUsuarioRequest request) {
        return ApiResponse.ok("Cuenta reactivada", usuarioService.reactivar(id,
                request == null ? null : request.motivo()));
    }

    @DeleteMapping("/api/admin/usuarios/{id}")
    public ApiResponse<Void> eliminar(@PathVariable Long id) {
        usuarioService.eliminar(id);
        return ApiResponse.ok("Usuario eliminado permanentemente. Los historiales y comprobantes se conservaron sin asociacion a esta cuenta.", null);
    }

    /**
     * Cartelera publica del equipo. Devuelve solo los datos de la ficha
     * profesional: el correo, el telefono y el estado de seguridad de la
     * cuenta (intentos fallidos, bloqueo) no se exponen a personas no
     * registradas.
     */
    @GetMapping("/api/veterinarios/publico")
    public ApiResponse<List<VeterinarioPublico>> veterinariosPublicos() {
        return ApiResponse.ok("Equipo veterinario", carteleras());
    }

    /** Cartelera del equipo: solo los datos de la ficha profesional. */
    private List<VeterinarioPublico> carteleras() {
        return usuarioService.veterinarios().stream()
                .map(v -> new VeterinarioPublico(
                        v.getId(),
                        v.getNombres(),
                        v.getApellidos(),
                        v.getEspecialidad(),
                        v.getCedulaProfesional(),
                        v.getTelefono(),
                        v.getEmail(),
                        v.getFotoUrl(),
                        v.getBiografia()))
                .toList();
    }

    /**
     * Mismo listado que la cartelera publica, pero para quien ya tiene
     * sesion. Se devuelve el DTO reducido (no la entidad {@code Usuario}) para
     * no filtrar el estado interno de las cuentas del equipo.
     */
    @GetMapping("/api/veterinarios")
    public ApiResponse<List<VeterinarioPublico>> veterinarios() {
        return ApiResponse.ok("Equipo veterinario", carteleras());
    }
}
