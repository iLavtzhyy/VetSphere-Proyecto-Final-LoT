package com.VetSphere.Backend.modules.clinic.controller;

import com.VetSphere.Backend.modules.clinic.domain.HistorialClinico;
import com.VetSphere.Backend.modules.clinic.repository.HistorialClinicoRepository;
import com.VetSphere.Backend.modules.clinic.service.ConsultaService;
import com.VetSphere.Backend.shared.response.ApiResponse;
import com.VetSphere.Backend.shared.response.Paginacion;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "Consultas", description = "Consultas clinicas registradas en el historial")
@RestController
@RequestMapping("/api/consultas")
@RequiredArgsConstructor
public class ConsultaController {
    private final ConsultaService consultaService;
    private final HistorialClinicoRepository historialRepository;

    @GetMapping
    @Operation(summary = "Lista consultas (paginacion opcional con page/size)")
    public ApiResponse<Object> listar(@RequestParam(required = false) Integer page,
                                      @RequestParam(required = false) Integer size) {
        if (Paginacion.solicitada(page, size)) {
            int pagina = Paginacion.pagina(page);
            int tamano = Paginacion.tamano(size);
            var resultado = historialRepository.findAll(PageRequest.of(pagina, tamano));
            return Paginacion.responder("Consultas", consultaService.mapear(resultado.getContent()),
                    resultado.getTotalElements(), page, size);
        }
        return Paginacion.responder("Consultas", consultaService.listar(),
                historialRepository.count(), null, null);
    }

    @PostMapping
    @Operation(summary = "Registra una consulta (la tarifa sale de TarifaService si no viene precio)")
    public ApiResponse<HistorialClinico> crear(@RequestBody HistorialClinico consulta) {
        return ApiResponse.ok("Consulta registrada", consultaService.crear(consulta));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualiza una consulta")
    public ApiResponse<HistorialClinico> editar(@PathVariable Long id, @RequestBody Map<String, Object> datos) {
        return ApiResponse.ok("Consulta actualizada", consultaService.editar(id, datos));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Elimina una consulta")
    public ApiResponse<Void> eliminar(@PathVariable Long id) {
        consultaService.eliminar(id);
        return ApiResponse.ok("Consulta eliminada", null);
    }
}
