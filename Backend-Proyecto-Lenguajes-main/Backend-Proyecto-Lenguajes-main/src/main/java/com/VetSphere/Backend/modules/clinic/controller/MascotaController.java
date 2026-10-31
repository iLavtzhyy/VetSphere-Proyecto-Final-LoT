package com.VetSphere.Backend.modules.clinic.controller;

import com.VetSphere.Backend.modules.clinic.dto.HistorialRequest;
import com.VetSphere.Backend.modules.clinic.dto.MascotaRequest;
import com.VetSphere.Backend.modules.clinic.domain.HistorialClinico;
import com.VetSphere.Backend.modules.clinic.domain.Mascota;
import com.VetSphere.Backend.shared.response.ApiResponse;
import com.VetSphere.Backend.shared.response.Paginacion;
import com.VetSphere.Backend.modules.clinic.service.MascotaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.security.core.Authentication;

import java.io.IOException;
import java.util.List;

@Tag(name = "Mascotas", description = "Pacientes de la clinica y su historial clinico")
@RestController
@RequestMapping("/api/mascotas")
@RequiredArgsConstructor
public class MascotaController {
    private final MascotaService mascotaService;

    @GetMapping
    @Operation(summary = "Lista mascotas (paginacion opcional con page/size; un cliente solo ve las suyas)")
    public ApiResponse<Object> listar(Authentication authentication,
                                      @RequestParam(required = false) Integer page,
                                      @RequestParam(required = false) Integer size) {
        List<Mascota> mascotas = mascotaService.listarParaUsuario(authentication);
        if (Paginacion.solicitada(page, size)) {
            return Paginacion.responder("Mascotas", mascotas, page, size);
        }
        return ApiResponse.ok("Mascotas", mascotas);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtiene una mascota por id")
    public ApiResponse<Mascota> obtener(@PathVariable Long id, Authentication authentication) {
        return ApiResponse.ok("Mascota", mascotaService.obtenerParaUsuario(id, authentication));
    }

    @PostMapping(consumes = "multipart/form-data")
    @Operation(summary = "Registra una mascota (multipart con foto opcional)")
    public ApiResponse<Mascota> crear(@Valid @RequestPart MascotaRequest mascota, @RequestPart(required = false) MultipartFile foto) throws IOException {
        return ApiResponse.ok("Mascota registrada", mascotaService.crear(mascota, foto));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualiza los datos de una mascota")
    public ApiResponse<Mascota> actualizar(@PathVariable Long id, @Valid @RequestBody MascotaRequest mascota) {
        return ApiResponse.ok("Mascota actualizada", mascotaService.actualizar(id, mascota));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Archiva una mascota y conserva su historial clinico")
    public ApiResponse<Void> eliminar(@PathVariable Long id) {
        mascotaService.eliminar(id);
        return ApiResponse.ok("Mascota archivada; el historial clinico se conserva", null);
    }

    @PostMapping(value = "/{id}/historial", consumes = "multipart/form-data")
    @Operation(summary = "Agrega un evento al historial clinico (archivo opcional)")
    public ApiResponse<HistorialClinico> agregarHistorial(
            @PathVariable Long id,
            @RequestPart HistorialRequest historial,
            @RequestPart(required = false) MultipartFile archivo) throws IOException {
        return ApiResponse.ok("Historial agregado", mascotaService.agregarHistorial(id, historial, archivo));
    }

    @GetMapping("/{id}/historial")
    @Operation(summary = "Historial clinico de una mascota (mas reciente primero)")
    public ApiResponse<List<HistorialClinico>> historial(@PathVariable Long id, Authentication authentication) {
        mascotaService.obtenerParaUsuario(id, authentication);
        return ApiResponse.ok("Historial clinico", mascotaService.historial(id));
    }

    @DeleteMapping("/{id}/historial/{historialId}/archivo")
    @Operation(summary = "Elimina el archivo adjunto de un evento del historial")
    public ApiResponse<Void> eliminarArchivo(@PathVariable Long id, @PathVariable Long historialId) throws IOException {
        mascotaService.eliminarArchivoHistorial(id, historialId);
        return ApiResponse.ok("Archivo eliminado", null);
    }
}


