package com.VetSphere.Backend.modules.clinic.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Datos de la ficha profesional de un veterinario.
 *
 * Antes el controlador recibia la entidad {@code Usuario} completa; con este
 * DTO nadie puede tocar el correo, la contrasena, el estado de la cuenta ni
 * los roles desde la ficha del equipo.
 */
public record VeterinarioRequest(
        @NotBlank(message = "los nombres son obligatorios")
        @Size(max = 80, message = "los nombres no pueden superar los 80 caracteres")
        String nombres,
        @NotBlank(message = "los apellidos son obligatorios")
        @Size(max = 80, message = "los apellidos no pueden superar los 80 caracteres")
        String apellidos,
        @Size(max = 30, message = "el telefono no puede superar los 30 caracteres")
        String telefono,
        @Size(max = 300, message = "la foto no puede superar los 300 caracteres")
        String fotoUrl,
        @Size(max = 120, message = "la especialidad no puede superar los 120 caracteres")
        String especialidad,
        @Size(max = 40, message = "la cedula profesional no puede superar los 40 caracteres")
        String cedulaProfesional,
        @Size(max = 1200, message = "la biografia no puede superar los 1200 caracteres")
        String biografia
) {
}
