package com.VetSphere.Backend.modules.clinic.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

import java.util.List;

/**
 * Pregunta que un usuario autenticado le hace al asistente inteligente.
 * El campo opcional `historial` lo envia el frontend para que el asistente
 * recuerde la conversacion; sin el, el comportamiento es identico al de
 * siempre (retrocompatible).
 */
public record ChatRequest(
        @NotBlank(message = "La pregunta para el asistente es obligatoria")
        String pregunta,
        List<@Valid MensajeChat> historial,
        Long conversacionId
) {
}
