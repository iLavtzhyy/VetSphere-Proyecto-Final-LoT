package com.VetSphere.Backend.modules.clinic.dto;

import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

/**
 * Datos de un ingreso por urgencia. Las validaciones de negocio (mascota y
 * motivo obligatorios, prioridad y estado dentro del catalogo) se resuelven en
 * UrgenciaService.
 */
public record UrgenciaRequest(
        Long mascotaId,
        LocalDateTime horaIngreso,
        @Size(max = 20, message = "La prioridad no puede superar los 20 caracteres")
        String prioridad,
        @Size(max = 300, message = "El motivo no puede superar los 300 caracteres")
        String motivo,
        @Size(max = 40, message = "El estado no puede superar los 40 caracteres")
        String estado,
        Long veterinarioId,
        @Size(max = 1200, message = "Las notas no pueden superar los 1200 caracteres")
        String notas
) {
}
