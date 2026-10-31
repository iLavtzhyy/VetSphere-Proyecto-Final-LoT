package com.VetSphere.Backend.modules.clinic.controller;

import com.VetSphere.Backend.modules.clinic.domain.Consentimiento;
import com.VetSphere.Backend.modules.clinic.dto.ConsentimientoRequest;
import com.VetSphere.Backend.modules.clinic.service.ConsentimientoService;
import com.VetSphere.Backend.shared.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/consentimientos")
@RequiredArgsConstructor
public class ConsentimientoController {
    private final ConsentimientoService consentimientoService;

    /**
     * Registro del consentimiento quirurgico. Mantiene el cuerpo historico
     * (mascotaId, cirugiaId, tipo, firmadoPor, cargo, documento, observaciones)
     * y ahora persiste el consentimiento con el folio generado en el servidor.
     */
    @PostMapping("/quirurgico")
    public ApiResponse<Consentimiento> registrarQuirurgico(@Valid @RequestBody(required = false) ConsentimientoRequest solicitud) {
        return ApiResponse.ok("Consentimiento registrado", consentimientoService.registrarQuirurgico(solicitud));
    }

    /** Lista los consentimientos con filtros opcionales por mascota y estado. */
    @GetMapping
    public ApiResponse<List<Consentimiento>> listar(
            @RequestParam(required = false) Long mascotaId,
            @RequestParam(required = false) String estado) {
        return ApiResponse.ok("Consentimientos", consentimientoService.listar(mascotaId, estado));
    }

    @GetMapping("/{id}")
    public ApiResponse<Consentimiento> obtener(@PathVariable Long id) {
        return ApiResponse.ok("Consentimiento", consentimientoService.obtener(id));
    }

    @PostMapping
    public ApiResponse<Consentimiento> crear(@Valid @RequestBody ConsentimientoRequest request) {
        return ApiResponse.ok("Consentimiento registrado", consentimientoService.crear(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<Consentimiento> actualizar(@PathVariable Long id, @Valid @RequestBody ConsentimientoRequest request) {
        return ApiResponse.ok("Consentimiento actualizado", consentimientoService.actualizar(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> eliminar(@PathVariable Long id) {
        consentimientoService.eliminar(id);
        return ApiResponse.ok("Consentimiento eliminado", null);
    }
}
