package com.VetSphere.Backend.modules.clinic.controller;

import com.VetSphere.Backend.modules.clinic.domain.EstudioImagen;
import com.VetSphere.Backend.modules.clinic.dto.EstudioImagenRequest;
import com.VetSphere.Backend.modules.clinic.service.ImagenologiaService;
import com.VetSphere.Backend.shared.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/imagenologia")
@RequiredArgsConstructor
public class ImagenologiaController {
    private final ImagenologiaService imagenologiaService;

    /** Estudios de imagenologia ordenados del mas reciente al mas antiguo. */
    @GetMapping("/estudios")
    public ApiResponse<List<EstudioImagen>> estudios() {
        return ApiResponse.ok("Estudios de imagenologia", imagenologiaService.listar());
    }

    @GetMapping
    public ApiResponse<List<EstudioImagen>> listar() {
        return ApiResponse.ok("Estudios de imagenologia", imagenologiaService.listar());
    }

    @GetMapping("/mascota/{mascotaId}")
    public ApiResponse<List<EstudioImagen>> porMascota(@PathVariable Long mascotaId) {
        return ApiResponse.ok("Estudios de imagenologia de la mascota", imagenologiaService.listarPorMascota(mascotaId));
    }

    @GetMapping("/{id}")
    public ApiResponse<EstudioImagen> obtener(@PathVariable Long id) {
        return ApiResponse.ok("Estudio de imagenologia", imagenologiaService.obtener(id));
    }

    @PostMapping
    public ApiResponse<EstudioImagen> crear(@Valid @RequestBody EstudioImagenRequest request) {
        return ApiResponse.ok("Estudio de imagenologia registrado", imagenologiaService.crear(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<EstudioImagen> actualizar(@PathVariable Long id, @Valid @RequestBody EstudioImagenRequest request) {
        return ApiResponse.ok("Estudio de imagenologia actualizado", imagenologiaService.actualizar(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> eliminar(@PathVariable Long id) {
        imagenologiaService.eliminar(id);
        return ApiResponse.ok("Estudio de imagenologia eliminado", null);
    }
}
