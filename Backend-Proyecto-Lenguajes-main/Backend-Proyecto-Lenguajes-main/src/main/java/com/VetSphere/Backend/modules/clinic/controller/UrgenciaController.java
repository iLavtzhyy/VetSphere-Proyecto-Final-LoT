package com.VetSphere.Backend.modules.clinic.controller;

import com.VetSphere.Backend.modules.clinic.domain.Urgencia;
import com.VetSphere.Backend.modules.clinic.dto.UrgenciaRequest;
import com.VetSphere.Backend.modules.clinic.service.UrgenciaService;
import com.VetSphere.Backend.shared.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Urgencias de la clinica: ingresos por emergencia y su evolucion.
 */
@RestController
@RequestMapping("/api/urgencias")
@RequiredArgsConstructor
public class UrgenciaController {
    private final UrgenciaService urgenciaService;

    @GetMapping("/activas")
    public ApiResponse<List<Urgencia>> activas() {
        return ApiResponse.ok("Urgencias activas", urgenciaService.activas());
    }

    @GetMapping
    public ApiResponse<List<Urgencia>> listar() {
        return ApiResponse.ok("Urgencias", urgenciaService.listar());
    }

    @PostMapping
    public ApiResponse<Urgencia> crear(@Valid @RequestBody UrgenciaRequest request) {
        return ApiResponse.ok("Urgencia registrada", urgenciaService.crear(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<Urgencia> actualizar(@PathVariable Long id, @Valid @RequestBody UrgenciaRequest request) {
        return ApiResponse.ok("Urgencia actualizada", urgenciaService.actualizar(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> eliminar(@PathVariable Long id) {
        urgenciaService.eliminar(id);
        return ApiResponse.ok("Urgencia eliminada", null);
    }
}
