package com.VetSphere.Backend.modules.clinic.controller;

import com.VetSphere.Backend.exception.ResourceNotFoundException;
import com.VetSphere.Backend.modules.clinic.domain.Usuario;
import com.VetSphere.Backend.modules.clinic.dto.VeterinarioRequest;
import com.VetSphere.Backend.modules.clinic.repository.UsuarioRepository;
import com.VetSphere.Backend.shared.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;

/**
 * Ficha del equipo veterinario.
 *
 * Solo se modifican los campos de la ficha profesional: el correo, la
 * contrasena, el estado de la cuenta y los roles se administran desde
 * {@code /api/admin/usuarios}.
 */
@RestController
@RequestMapping("/api/equipo-veterinario")
@RequiredArgsConstructor
public class VeterinarioController {
    private final UsuarioRepository usuarioRepository;

    @GetMapping
    public ApiResponse<List<Usuario>> listar() {
        return ApiResponse.ok("Veterinarios", usuarioRepository.findByRolesNombre("ROLE_VETERINARIO"));
    }

    @PutMapping("/{id}")
    public ApiResponse<Usuario> actualizar(@PathVariable Long id, @Valid @RequestBody VeterinarioRequest datos) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("El veterinario solicitado no existe."));
        usuario.setNombres(datos.nombres().trim());
        usuario.setApellidos(datos.apellidos().trim());
        usuario.setTelefono(datos.telefono());
        usuario.setFotoUrl(datos.fotoUrl());
        usuario.setEspecialidad(datos.especialidad());
        usuario.setCedulaProfesional(datos.cedulaProfesional());
        usuario.setBiografia(datos.biografia());
        return ApiResponse.ok("Veterinario actualizado", usuarioRepository.save(usuario));
    }
}
