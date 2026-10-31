package com.VetSphere.Backend.modules.clinic.dto;

import jakarta.validation.constraints.Size;

/** Payload para activar o desactivar manualmente la cuenta de un usuario. */
public record EstadoUsuarioRequest(
        @Size(max = 300, message = "El motivo es demasiado extenso") String motivo
) {
}
