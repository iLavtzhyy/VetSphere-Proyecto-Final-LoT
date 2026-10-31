package com.VetSphere.Backend.modules.clinic.controller;

import com.VetSphere.Backend.modules.clinic.repository.*;
import com.VetSphere.Backend.shared.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
import com.VetSphere.Backend.modules.clinic.service.AuthService;

@RestController
@RequestMapping("/api/administracion")
@RequiredArgsConstructor
public class AdministracionController {
    private final UsuarioRepository usuarioRepository;
    private final MascotaRepository mascotaRepository;
    private final ProductoRepository productoRepository;
    private final FacturaRepository facturaRepository;
    private final SolicitudRegistroRepository solicitudRegistroRepository;
    private final AuthService authService;

    @GetMapping("/codigo-registro")
    public ApiResponse<Map<String, Object>> codigoRegistro() {
        return ApiResponse.ok("Codigo de autorizacion", Map.of("codigo", authService.codigoRegistroActual(), "segundosRestantes", authService.segundosCodigoRegistro()));
    }

    @GetMapping("/indicadores")
    public ApiResponse<Map<String, Object>> indicadores() {
        return ApiResponse.ok("Indicadores administrativos", Map.of(
                "usuarios", usuarioRepository.count(),
                "mascotas", mascotaRepository.count(),
                "productos", productoRepository.count(),
                "facturas", facturaRepository.count(),
                "solicitudes", solicitudRegistroRepository.count()));
    }
}

