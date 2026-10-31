package com.VetSphere.Backend.modules.clinic.controller;

import com.VetSphere.Backend.shared.response.ApiResponse;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Catalogo de especialidades de la clinica.
 *
 * A diferencia de los modulos que antes devolvian datos inventados, este es
 * un catalogo de referencia: describe las areas clinicas que atiende la
 * clinica y no depende de ninguna tabla. Si en el futuro cada especialidad
 * necesita ficha propia (equipo, tarifas, agenda), pasa a persistirse.
 */
@RestController
@RequestMapping("/api/especialidades")
public class EspecialidadController {

    private static final List<String> ESPECIALIDADES = List.of(
            "Medicina interna",
            "Cirugia",
            "Traumatologia",
            "Dermatologia",
            "Oftalmologia",
            "Cardiologia",
            "Animales exoticos",
            "Laboratorio");

    @GetMapping
    public ApiResponse<List<String>> listar() {
        return ApiResponse.ok("Especialidades", ESPECIALIDADES);
    }
}
