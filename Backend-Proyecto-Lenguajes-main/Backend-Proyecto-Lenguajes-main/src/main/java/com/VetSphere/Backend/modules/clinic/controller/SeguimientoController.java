package com.VetSphere.Backend.modules.clinic.controller;

import com.VetSphere.Backend.modules.clinic.domain.Seguimiento;
import com.VetSphere.Backend.modules.clinic.dto.SeguimientoRequest;
import com.VetSphere.Backend.modules.clinic.service.SeguimientoService;
import com.VetSphere.Backend.shared.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Seguimientos clinicos de las mascotas (postoperatorios, controles,etc.).
 */
@RestController
@RequestMapping("/api/seguimientos")
@RequiredArgsConstructor
public class SeguimientoController {
    private final SeguimientoService seguimientoService;

    @GetMapping("/postoperatorios")
    public ApiResponse<List<Seguimiento>> postoperatorios() {
        return ApiResponse.ok("Seguimientos postoperatorios", seguimientoService.postoperatorios());
    }

    @GetMapping
    public ApiResponse<List<Seguimiento>> listar() {
        return ApiResponse.ok("Seguimientos", seguimientoService.listar());
    }

    @PostMapping
    public ApiResponse<Seguimiento> crear(@Valid @RequestBody SeguimientoRequest request) {
        return ApiResponse.ok("Seguimiento registrado", seguimientoService.crear(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<Seguimiento> actualizar(@PathVariable Long id, @Valid @RequestBody SeguimientoRequest request) {
        return ApiResponse.ok("Seguimiento actualizado", seguimientoService.actualizar(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> eliminar(@PathVariable Long id) {
        seguimientoService.eliminar(id);
        return ApiResponse.ok("Seguimiento eliminado", null);
    }
}
