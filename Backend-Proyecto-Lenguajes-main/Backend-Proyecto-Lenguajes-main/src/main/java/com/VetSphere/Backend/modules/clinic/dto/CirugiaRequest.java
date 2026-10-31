package com.VetSphere.Backend.modules.clinic.dto;

import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CirugiaRequest(
        Long mascotaId,
        Long cirujanoId,
        LocalDateTime fechaProgramada,
        String procedimiento,
        String tipoAnestesia,
        String cirujanoPrincipal,
        String constantesVitales,
        String estadoPostoperatorio,
        String estado,
        String notas,
        @Positive(message = "El precio de la cirugia debe ser un valor positivo")
        BigDecimal precio
) {
}
