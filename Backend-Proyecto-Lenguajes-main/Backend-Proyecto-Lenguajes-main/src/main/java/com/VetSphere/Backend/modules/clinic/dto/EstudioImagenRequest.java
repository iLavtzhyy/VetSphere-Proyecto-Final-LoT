package com.VetSphere.Backend.modules.clinic.dto;

import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record EstudioImagenRequest(
        Long mascotaId,
        @Size(max = 50, message = "El tipo de estudio no puede superar los 50 caracteres")
        String tipo,
        LocalDate fecha,
        @Size(max = 50, message = "El estado del estudio no puede superar los 50 caracteres")
        String estado,
        @Size(max = 1800, message = "El informe no puede superar los 1800 caracteres")
        String informe,
        Long veterinarioId,
        @Size(max = 500, message = "La direccion del archivo no puede superar los 500 caracteres")
        String archivoUrl
) {
}
