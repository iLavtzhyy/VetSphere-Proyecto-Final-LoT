package com.VetSphere.Backend.modules.clinic.controller;

import com.VetSphere.Backend.modules.clinic.repository.*;
import com.VetSphere.Backend.shared.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/estadisticas")
@RequiredArgsConstructor
public class EstadisticaController {
    private final MascotaRepository mascotaRepository;
    private final UsuarioRepository usuarioRepository;
    private final CirugiaRepository cirugiaRepository;
    private final ProductoRepository productoRepository;

    @GetMapping("/generales")
    public ApiResponse<Map<String, Object>> generales() {
        return ApiResponse.ok("Estadisticas generales", Map.of(
                "mascotas", mascotaRepository.count(),
                "usuarios", usuarioRepository.count(),
                "cirugias", cirugiaRepository.count(),
                "productos", productoRepository.count()));
    }
}

