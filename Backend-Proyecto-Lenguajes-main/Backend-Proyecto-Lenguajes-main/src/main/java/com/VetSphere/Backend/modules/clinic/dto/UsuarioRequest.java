package com.VetSphere.Backend.modules.clinic.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.Set;

public record UsuarioRequest(
        @Size(max = 150, message = "Los nombres no pueden superar los 150 caracteres") String nombres,
        @Size(max = 150, message = "Los apellidos no pueden superar los 150 caracteres") String apellidos,
        @Email(message = "El correo del usuario no es valido")
        @Size(max = 150) String email,
        @Size(max = 30) String telefono,
        @Size(max = 72, message = "La contrasena no puede superar 72 caracteres")
        @Pattern(regexp = "^(?:$|(?=.{8,72}$)(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).*)",
                message = "La contrasena debe incluir minuscula, mayuscula y un digito") String password,
        @Size(max = 500) String fotoUrl,
        @Size(max = 150) String especialidad,
        @Size(max = 30) String cedulaProfesional,
        @Size(max = 1000) String biografia,
        Set<String> roles,
        Boolean activo
) {
}
