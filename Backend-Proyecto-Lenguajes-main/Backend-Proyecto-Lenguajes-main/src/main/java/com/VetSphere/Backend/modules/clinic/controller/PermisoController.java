package com.VetSphere.Backend.modules.clinic.controller;

import com.VetSphere.Backend.security.ReglasSeguridad;
import com.VetSphere.Backend.shared.response.ApiResponse;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Matriz de permisos por rol expuesta a la interfaz.
 *
 * <p>No es una lista escrita a mano: se deduce de
 * {@link ReglasSeguridad}, la misma tabla que construye la cadena de
 * seguridad de Spring Security. Gracias a eso la interfaz nunca muestra una
 * accion que el backend vaya a rechazar (ni esconde una que si valida).</p>
 */
@RestController
@RequestMapping("/api/permisos")
public class PermisoController {

    @GetMapping
    public ApiResponse<Map<String, List<String>>> listarPermisosPorRol() {
        return ApiResponse.ok("Permisos por rol", permisosPorRol());
    }

    /**
     * Modulos accesibles por cada rol segun las reglas de seguridad.
     */
    public static Map<String, List<String>> permisosPorRol() {
        return ReglasSeguridad.modulosPorRol();
    }
}
