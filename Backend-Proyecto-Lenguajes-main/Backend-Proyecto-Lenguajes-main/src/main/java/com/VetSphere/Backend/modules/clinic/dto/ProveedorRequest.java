package com.VetSphere.Backend.modules.clinic.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ProveedorRequest(
        @NotBlank(message = "El nombre del proveedor es obligatorio") @Size(max = 150) String nombre,
        @Size(max = 150) String contacto,
        @Size(max = 30) String telefono,
        @Email(message = "El correo del proveedor no es valido") String email,
        @Size(max = 250) String direccion,
        Boolean activo
) {
}
