package com.VetSphere.Backend.modules.clinic.controller;

import com.VetSphere.Backend.modules.clinic.domain.Vacunacion;
import com.VetSphere.Backend.modules.clinic.dto.VacunacionRequest;
import com.VetSphere.Backend.modules.clinic.service.VacunacionService;
import com.VetSphere.Backend.shared.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Calendario de vacunaciones de la clinica.
 */
@RestController
@RequestMapping("/api/vacunacion")
@RequiredArgsConstructor
public class VacunacionController {
    private final VacunacionService vacunacionService;

    @GetMapping("/calendario")
    public ApiResponse<List<Vacunacion>> calendario() {
        return ApiResponse.ok("Calendario de vacunacion", vacunacionService.calendario());
    }

    @GetMapping
    public ApiResponse<List<Vacunacion>> listar() {
        return ApiResponse.ok("Vacunaciones", vacunacionService.listar());
    }

    @PostMapping
    public ApiResponse<Vacunacion> crear(@Valid @RequestBody VacunacionRequest request) {
        return ApiResponse.ok("Vacunacion registrada", vacunacionService.crear(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<Vacunacion> actualizar(@PathVariable Long id, @Valid @RequestBody VacunacionRequest request) {
        return ApiResponse.ok("Vacunacion actualizada", vacunacionService.actualizar(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> eliminar(@PathVariable Long id) {
        vacunacionService.eliminar(id);
        return ApiResponse.ok("Vacunacion eliminada", null);
    }
}
