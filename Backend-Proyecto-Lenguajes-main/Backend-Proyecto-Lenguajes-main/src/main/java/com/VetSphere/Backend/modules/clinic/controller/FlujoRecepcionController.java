package com.VetSphere.Backend.modules.clinic.controller;

import com.VetSphere.Backend.modules.clinic.domain.EtapaFlujoRecepcion;
import com.VetSphere.Backend.modules.clinic.dto.EtapaFlujoRecepcionRequest;
import com.VetSphere.Backend.modules.clinic.service.FlujoRecepcionService;
import com.VetSphere.Backend.shared.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/recepcion/flujo")
@RequiredArgsConstructor
public class FlujoRecepcionController {
    private final FlujoRecepcionService flujoService;

    @GetMapping
    public ApiResponse<List<EtapaFlujoRecepcion>> listar() {
        return ApiResponse.ok("Etapas del flujo de recepcion", flujoService.listar());
    }

    @PostMapping
    public ApiResponse<EtapaFlujoRecepcion> crear(@Valid @RequestBody EtapaFlujoRecepcionRequest request) {
        return ApiResponse.ok("Etapa creada", flujoService.crear(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<EtapaFlujoRecepcion> actualizar(@PathVariable Long id,
                                                       @Valid @RequestBody EtapaFlujoRecepcionRequest request) {
        return ApiResponse.ok("Etapa actualizada", flujoService.actualizar(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<EtapaFlujoRecepcion> eliminar(@PathVariable Long id) {
        return ApiResponse.ok("Etapa eliminada", flujoService.eliminar(id));
    }
}
