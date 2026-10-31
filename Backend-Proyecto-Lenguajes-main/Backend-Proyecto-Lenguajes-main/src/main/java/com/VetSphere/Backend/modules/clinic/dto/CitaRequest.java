package com.VetSphere.Backend.modules.clinic.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

/**
 * Datos admitidos para crear o actualizar una cita.
 *
 * Antes el controlador recibia la entidad {@code Cita} completa: eso permitia
 * enviar un {@code id} y sobrescribir citas ajenas. Aqui solo se aceptan los
 * campos del formulario y las referencias se resuelven por id en el servidor.
 */
public record CitaRequest(
        /** Referencia a la entidad relacionada (solo se usa el id). */
        ReferenciaRequest mascota,
        ReferenciaRequest veterinario,
        ReferenciaRequest recepcionista,
        @NotNull(message = "la fecha y hora de la cita son obligatorias")
        LocalDateTime fechaHora,
        @NotBlank(message = "el motivo de la cita es obligatorio")
        @Size(max = 300, message = "el motivo no puede superar los 300 caracteres")
        String motivo,
        @Size(max = 60, message = "el estado no puede superar los 60 caracteres")
        String estado,
        @Size(max = 600, message = "las observaciones no pueden superar los 600 caracteres")
        String observaciones
) {

    /** Referencia minima a una entidad relacionada. */
    public record ReferenciaRequest(Long id) {
    }
}
