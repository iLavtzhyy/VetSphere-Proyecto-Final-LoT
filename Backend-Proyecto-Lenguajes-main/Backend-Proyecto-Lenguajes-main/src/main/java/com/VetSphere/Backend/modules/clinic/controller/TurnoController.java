package com.VetSphere.Backend.modules.clinic.controller;

import com.VetSphere.Backend.modules.clinic.domain.Turno;
import com.VetSphere.Backend.modules.clinic.dto.TurnoRequest;
import com.VetSphere.Backend.modules.clinic.service.TurnoService;
import com.VetSphere.Backend.shared.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * CRUD de los turnos del personal (persistidos en base de datos).
 * Antes era un facade con datos de demostracion hardcodeados.
 */
@Tag(name = "Turnos", description = "Gestion de los turnos del personal de la clinica (solo ADMIN)")
@RestController
@RequestMapping("/api/turnos")
@RequiredArgsConstructor
public class TurnoController {
    private final TurnoService turnoService;

    @GetMapping
    @Operation(summary = "Lista los turnos ordenados por fecha y hora de inicio")
    public ApiResponse<List<Turno>> listar() {
        return ApiResponse.ok("Turnos del personal", turnoService.listar());
    }

    @PostMapping
    @Operation(summary = "Crea un turno validando al empleado y el solapamiento de horarios")
    public ApiResponse<Turno> crear(@Valid @RequestBody TurnoRequest request) {
        return ApiResponse.ok("Turno registrado", turnoService.crear(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualiza un turno existente")
    public ApiResponse<Turno> actualizar(@PathVariable Long id, @Valid @RequestBody TurnoRequest request) {
        return ApiResponse.ok("Turno actualizado", turnoService.actualizar(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Elimina un turno")
    public ApiResponse<Void> eliminar(@PathVariable Long id) {
        turnoService.eliminar(id);
        return ApiResponse.ok("Turno eliminado", null);
    }
}
