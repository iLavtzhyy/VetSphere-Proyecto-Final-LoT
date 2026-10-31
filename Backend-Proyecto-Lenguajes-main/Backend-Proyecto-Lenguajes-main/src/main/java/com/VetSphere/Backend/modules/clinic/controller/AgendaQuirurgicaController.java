package com.VetSphere.Backend.modules.clinic.controller;

import com.VetSphere.Backend.modules.clinic.domain.Cirugia;
import com.VetSphere.Backend.shared.response.ApiResponse;
import com.VetSphere.Backend.modules.clinic.service.CirugiaService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/agenda-quirurgica")
@RequiredArgsConstructor
public class AgendaQuirurgicaController {
    private final CirugiaService cirugiaService;

    @GetMapping("/hoy")
    public ApiResponse<List<Cirugia>> cirugiasDelDia() {
        return ApiResponse.ok("Agenda quirurgica del dia", cirugiaService.hoy());
    }
}

