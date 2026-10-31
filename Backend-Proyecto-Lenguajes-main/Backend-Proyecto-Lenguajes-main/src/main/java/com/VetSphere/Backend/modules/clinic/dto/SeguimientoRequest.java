package com.VetSphere.Backend.modules.clinic.dto;

import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * Datos de un seguimiento clinico. Las validaciones de negocio (mascota e
 * indicacion obligatorias, tipo y estado dentro del catalogo) se resuelven en
 * SeguimientoService.
 */
public record SeguimientoRequest(
        Long mascotaId,
        LocalDate fecha,
        @Size(max = 200, message = "La indicacion no puede superar los 200 caracteres")
        String indicacion,
        @Size(max = 40, message = "El tipo no puede superar los 40 caracteres")
        String tipo,
        @Size(max = 40, message = "El estado no puede superar los 40 caracteres")
        String estado,
        Long veterinarioId,
        @Size(max = 1200, message = "Las observaciones no pueden superar los 1200 caracteres")
        String observaciones
) {
}
