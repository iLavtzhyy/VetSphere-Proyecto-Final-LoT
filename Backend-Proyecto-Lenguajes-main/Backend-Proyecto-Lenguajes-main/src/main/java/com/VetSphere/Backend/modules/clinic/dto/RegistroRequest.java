package com.VetSphere.Backend.modules.clinic.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Payload de registro. La contrasena debe cumplir una politica minima:
 * 8 caracteres, una mayuscula, una minuscula y un digito.
 */
public record RegistroRequest(
        @NotBlank(message = "Los nombres son obligatorios") @Size(max = 80, message = "Los nombres son demasiado extensos") String nombres,
        @NotBlank(message = "Los apellidos son obligatorios") @Size(max = 80, message = "Los apellidos son demasiado extensos") String apellidos,
        @NotBlank(message = "El correo es obligatorio") @Email(message = "El correo no tiene un formato valido") String email,
        @Size(max = 30, message = "El telefono es demasiado extenso") String telefono,
        @NotBlank(message = "La contrasena es obligatoria")
        @Size(min = 8, max = 72, message = "La contrasena debe tener entre 8 y 72 caracteres")
        @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).+$",
                message = "La contrasena debe incluir minuscula, mayuscula y un digito") String password,
        @Pattern(regexp = "^$|[0-9]{10}", message = "La cedula debe tener 10 digitos") String documento,
        String rolSolicitado,
        @Size(max = 120) String codigoAdmin
) {
}
