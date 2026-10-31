package com.VetSphere.Backend.modules.clinic.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Payload de creacion y actualizacion de turnos del personal.
 *
 * El empleado se envia por id ({@code empleadoId}) porque la entidad guarda la
 * relacion ManyToOne con el usuario de la clinica.
 */
public record TurnoRequest(
        @NotNull(message = "Debes indicar el empleado del turno")
        Long empleadoId,
        @NotNull(message = "Debes indicar la fecha del turno")
        LocalDate fecha,
        @NotNull(message = "Debes indicar la hora de inicio del turno")
        LocalTime horaInicio,
        @NotNull(message = "Debes indicar la hora de fin del turno")
        LocalTime horaFin,
        @NotBlank(message = "El area del turno es obligatoria")
        @Size(max = 80, message = "El area del turno no puede superar los 80 caracteres")
        String area,
        @Size(max = 600, message = "Las notas del turno no pueden superar los 600 caracteres")
        String notas
) {
}
