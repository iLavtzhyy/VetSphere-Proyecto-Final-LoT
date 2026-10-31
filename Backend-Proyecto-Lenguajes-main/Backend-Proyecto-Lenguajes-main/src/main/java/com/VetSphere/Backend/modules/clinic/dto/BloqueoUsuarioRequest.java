package com.VetSphere.Backend.modules.clinic.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

/** Payload para bloquear o desbloquear manualmente la cuenta de un usuario. */
public record BloqueoUsuarioRequest(
        @Min(value = 1, message = "El bloqueo debe durar al menos 1 hora")
        @Max(value = 720, message = "El bloqueo no puede superar 720 horas (30 dias)")
        Integer horas,
        @Size(max = 300, message = "El motivo es demasiado extenso") String motivo
) {
}
