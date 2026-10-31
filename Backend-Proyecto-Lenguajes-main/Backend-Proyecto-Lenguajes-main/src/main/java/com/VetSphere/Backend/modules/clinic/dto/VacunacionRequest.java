package com.VetSphere.Backend.modules.clinic.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * Datos de una vacunacion. Las validaciones de negocio (mascota obligatoria,
 * estado dentro del catalogo) se resuelven en VacunacionService.
 */
public record VacunacionRequest(
        Long mascotaId,
        @Size(max = 120, message = "El nombre de la vacuna no puede superar los 120 caracteres")
        @JsonAlias({"vaccine", "nombreVacuna"})
        String vacuna,
        LocalDate fecha,
        LocalDate fechaProxima,
        @Size(max = 60, message = "El lote no puede superar los 60 caracteres")
        String lote,
        @Size(max = 40, message = "El estado no puede superar los 40 caracteres")
        String estado,
        @Size(max = 1200, message = "Las observaciones no pueden superar los 1200 caracteres")
        String observaciones
) {
}
