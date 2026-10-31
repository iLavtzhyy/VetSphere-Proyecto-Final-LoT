package com.VetSphere.Backend.modules.clinic.controller;

import com.VetSphere.Backend.modules.clinic.dto.CirugiaRequest;
import com.VetSphere.Backend.modules.clinic.domain.Cirugia;
import com.VetSphere.Backend.shared.response.ApiResponse;
import com.VetSphere.Backend.modules.clinic.service.CirugiaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cirugias")
@RequiredArgsConstructor
public class CirugiaController {
    private final CirugiaService cirugiaService;

    @GetMapping
    public ApiResponse<List<Cirugia>> listar() {
        return ApiResponse.ok("Cirugias", cirugiaService.listar());
    }

    @GetMapping("/hoy")
    public ApiResponse<List<Cirugia>> hoy() {
        return ApiResponse.ok("Cirugias de hoy", cirugiaService.hoy());
    }

    @PostMapping
    public ApiResponse<Cirugia> crear(@Valid @RequestBody CirugiaRequest request) {
        return ApiResponse.ok("Cirugia programada", cirugiaService.crear(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<Cirugia> actualizar(@PathVariable Long id, @Valid @RequestBody CirugiaRequest request) {
        return ApiResponse.ok("Cirugia actualizada", cirugiaService.actualizar(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> eliminar(@PathVariable Long id) {
        cirugiaService.eliminar(id);
        return ApiResponse.ok("Cirugia eliminada", null);
    }
}


