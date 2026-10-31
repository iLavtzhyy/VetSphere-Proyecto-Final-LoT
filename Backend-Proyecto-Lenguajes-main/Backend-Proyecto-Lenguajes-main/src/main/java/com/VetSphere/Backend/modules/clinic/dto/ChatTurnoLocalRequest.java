package com.VetSphere.Backend.modules.clinic.dto;

import jakarta.validation.constraints.NotBlank;

public record ChatTurnoLocalRequest(Long conversacionId,
                                    @NotBlank String pregunta,
                                    @NotBlank String respuesta) {
}
