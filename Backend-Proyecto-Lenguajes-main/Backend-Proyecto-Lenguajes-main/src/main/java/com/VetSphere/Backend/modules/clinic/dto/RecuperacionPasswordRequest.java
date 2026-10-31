package com.VetSphere.Backend.modules.clinic.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Payload simple con el correo de referencia (reenvio de codigo, recuperacion). */
public record RecuperacionPasswordRequest(
        @NotBlank(message = "El correo es obligatorio") @Email(message = "El correo no tiene un formato valido") String email) {
}
