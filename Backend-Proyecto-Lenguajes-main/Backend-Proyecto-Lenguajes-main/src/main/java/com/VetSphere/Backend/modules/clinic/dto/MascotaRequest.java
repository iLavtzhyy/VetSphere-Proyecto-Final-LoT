package com.VetSphere.Backend.modules.clinic.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record MascotaRequest(
        @NotBlank(message = "El nombre de la mascota es obligatorio")
        @Size(max = 100, message = "El nombre de la mascota no puede superar los 100 caracteres")
        String nombre,
        @Size(max = 100) String especie,
        @Size(max = 100) String raza,
        @Size(max = 30) String sexo,
        @Size(max = 100) String color,
        Double pesoKg,
        LocalDate fechaNacimiento,
        Long propietarioId,
        @Size(max = 30) String estado
) {
}
