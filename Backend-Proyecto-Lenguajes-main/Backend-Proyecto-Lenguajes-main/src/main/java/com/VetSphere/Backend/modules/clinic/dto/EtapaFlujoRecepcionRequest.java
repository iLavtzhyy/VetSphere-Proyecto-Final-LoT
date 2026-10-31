package com.VetSphere.Backend.modules.clinic.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record EtapaFlujoRecepcionRequest(
        @NotBlank(message = "El nombre de la etapa es obligatorio")
        @Size(max = 60, message = "El nombre no puede superar 60 caracteres")
        String nombre,

        @NotBlank(message = "El estado de la cita es obligatorio")
        @Size(max = 60, message = "El estado no puede superar 60 caracteres")
        String estadoCita,

        @Size(max = 180, message = "La descripcion no puede superar 180 caracteres")
        String descripcion,

        @NotBlank(message = "Selecciona un color")
        @Pattern(regexp = "cyan|emerald|amber|rose|violet|blue|slate", message = "El color seleccionado no es valido")
        String color,

        @Min(value = 1, message = "La posicion debe ser mayor que cero")
        @Max(value = 50, message = "El flujo admite hasta 50 etapas")
        Integer posicion,

        Boolean activa
) {
}
