package com.VetSphere.Backend.modules.clinic.controller;

import com.VetSphere.Backend.modules.clinic.domain.AuditorEvento;
import com.VetSphere.Backend.modules.clinic.repository.AuditorEventoRepository;
import com.VetSphere.Backend.shared.response.ApiResponse;
import com.VetSphere.Backend.shared.response.Paginacion;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Consulta de los eventos de auditoria reales almacenados en base de datos.
 * (Antes devolvia tres eventos inventados en memoria.)
 * Acceso restringido a ADMIN por SecurityConfig.
 */
@Tag(name = "Auditoria", description = "Eventos de auditoria de la plataforma (solo ADMIN)")
@RestController
@RequestMapping("/api/auditoria")
@RequiredArgsConstructor
public class AuditoriaController {
    private final AuditorEventoRepository auditorEventoRepository;

    @GetMapping("/eventos")
    @Operation(summary = "Lista eventos de auditoria (paginacion opcional con page/size)")
    public ApiResponse<Object> eventos(@RequestParam(required = false) Integer page,
                                       @RequestParam(required = false) Integer size) {
        if (Paginacion.solicitada(page, size)) {
            int pagina = Paginacion.pagina(page);
            int tamano = Paginacion.tamano(size);
            var resultado = auditorEventoRepository.listarPaginado(PageRequest.of(pagina, tamano));
            return Paginacion.responder("Eventos de auditoria", resultado.getContent(),
                    resultado.getTotalElements(), page, size);
        }
        List<AuditorEvento> eventos = auditorEventoRepository.findTop200ByOrderByFechaDesc();
        return Paginacion.responder("Eventos de auditoria", eventos, eventos.size(), null, null);
    }
}
