package com.VetSphere.Backend.modules.clinic.controller;

import com.VetSphere.Backend.modules.clinic.domain.Cita;
import com.VetSphere.Backend.modules.clinic.dto.CitaRequest;
import com.VetSphere.Backend.modules.clinic.repository.CitaRepository;
import com.VetSphere.Backend.modules.clinic.repository.UsuarioRepository;
import com.VetSphere.Backend.modules.clinic.service.CitaService;
import com.VetSphere.Backend.shared.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/recepcion")
@RequiredArgsConstructor
public class RecepcionController {
    private final CitaRepository citaRepository;
    private final UsuarioRepository usuarioRepository;
    private final CitaService citaService;

    @GetMapping("/panel")
    public ApiResponse<Map<String, Object>> panel() {
        return ApiResponse.ok("Panel recepcion", Map.of(
                "citas", citaRepository.count(),
                "clientes", usuarioRepository.findByRolesNombre("ROLE_CLIENTE").size(),
                "recepcionistas", usuarioRepository.findByRolesNombre("ROLE_RECEPCIONISTA").size()));
    }

    /**
     * Crea una cita desde recepcion. Acepta el mismo cuerpo que
     * {@code POST /api/citas} y delega las validaciones en el servicio.
     */
    @PostMapping("/citas")
    public ApiResponse<Cita> crearCita(@Valid @RequestBody CitaRequest request) {
        return ApiResponse.ok("Cita creada desde recepcion", citaService.crear(request));
    }
}
