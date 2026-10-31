package com.VetSphere.Backend.modules.clinic.dto;

import java.time.LocalDateTime;

public record HistorialRequest(
        LocalDateTime fecha,
        String tipoEvento,
        String motivo,
        String diagnostico,
        String tratamiento,
        String vacunas,
        Double temperatura,
        Integer frecuenciaCardiaca,
        Integer frecuenciaRespiratoria,
        Long veterinarioId
) {
}


