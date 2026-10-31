package com.VetSphere.Backend.modules.clinic.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Payload con el correo del titular y el codigo recibido por email.
 * Se usa para desactivar y reactivar la cuenta propia.
 */
public record CodigoRequest(
        @NotBlank(message = "El correo es obligatorio") @Email(message = "El correo no tiene un formato valido") String email,
        @NotBlank(message = "El codigo es obligatorio")
        @Pattern(regexp = "\\d{6}", message = "El codigo debe tener 6 digitos") String codigo
) {
}
