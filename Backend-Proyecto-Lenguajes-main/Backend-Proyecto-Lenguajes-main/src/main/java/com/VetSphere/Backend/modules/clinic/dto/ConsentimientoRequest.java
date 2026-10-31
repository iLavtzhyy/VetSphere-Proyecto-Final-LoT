package com.VetSphere.Backend.modules.clinic.dto;

import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record ConsentimientoRequest(
        Long mascotaId,
        Long cirugiaId,
        @Size(max = 50, message = "El tipo de consentimiento no puede superar los 50 caracteres")
        String tipo,
        @Size(max = 150, message = "El nombre de quien firma no puede superar los 150 caracteres")
        String firmadoPor,
        @Size(max = 80, message = "El cargo no puede superar los 80 caracteres")
        String cargo,
        LocalDateTime fecha,
        @Size(max = 50, message = "El estado del consentimiento no puede superar los 50 caracteres")
        String estado,
        @Size(max = 4000, message = "El documento no puede superar los 4000 caracteres")
        String documento,
        @Size(max = 1000, message = "Las observaciones no pueden superar los 1000 caracteres")
        String observaciones
) {
}
