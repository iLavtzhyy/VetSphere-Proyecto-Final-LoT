package com.VetSphere.Backend.modules.clinic.controller;

import com.VetSphere.Backend.modules.clinic.service.TriageService;
import com.VetSphere.Backend.shared.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Triage de urgencias: clasifica al paciente con las reglas clinicas de
 * {@link TriageService} y explica los motivos de la clasificacion.
 */
@RestController
@RequestMapping("/api/triage")
@RequiredArgsConstructor
public class TriageController {
    private final TriageService triageService;

    @PostMapping("/clasificar")
    public ApiResponse<Map<String, Object>> clasificar(@RequestBody(required = false) Map<String, Object> signos) {
        return ApiResponse.ok("Paciente clasificado", triageService.detalle(signos));
    }
}
