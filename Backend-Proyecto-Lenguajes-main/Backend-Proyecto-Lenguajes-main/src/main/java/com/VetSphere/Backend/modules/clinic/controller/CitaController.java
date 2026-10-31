package com.VetSphere.Backend.modules.clinic.controller;

import com.VetSphere.Backend.modules.clinic.domain.Cita;
import com.VetSphere.Backend.modules.clinic.dto.CitaRequest;
import com.VetSphere.Backend.modules.clinic.repository.CitaRepository;
import com.VetSphere.Backend.modules.clinic.service.CitaService;
import com.VetSphere.Backend.shared.response.ApiResponse;
import com.VetSphere.Backend.shared.response.Paginacion;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Citas", description = "Programacion de citas de la clinica")
@RestController
@RequestMapping("/api/citas")
@RequiredArgsConstructor
public class CitaController {
    private final CitaRepository citaRepository;
    private final CitaService citaService;

    @GetMapping
    @Operation(summary = "Lista citas (paginacion opcional con page/size)")
    public ApiResponse<Object> listar(@RequestParam(required = false) Integer page,
                                      @RequestParam(required = false) Integer size) {
        if (Paginacion.solicitada(page, size)) {
            var resultado = citaRepository.findAll(PageRequest.of(Paginacion.pagina(page), Paginacion.tamano(size)));
            return Paginacion.responder("Citas", resultado.getContent(), resultado.getTotalElements(), page, size);
        }
        return Paginacion.responder("Citas", citaRepository.findAll(), null, null);
    }

    @PostMapping
    @Operation(summary = "Crea una cita")
    public ApiResponse<Cita> crear(@Valid @RequestBody CitaRequest request) {
        return ApiResponse.ok("Cita creada", citaService.crear(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualiza una cita")
    public ApiResponse<Cita> actualizar(@PathVariable Long id, @Valid @RequestBody CitaRequest request) {
        return ApiResponse.ok("Cita actualizada", citaService.actualizar(id, request));
    }

    @GetMapping("/estados")
    @Operation(summary = "Estados validos de una cita")
    public ApiResponse<List<String>> estados() {
        return ApiResponse.ok("Estados de cita", citaService.estadosDisponibles());
    }
}
