package com.VetSphere.Backend.modules.clinic.dto;

import java.time.LocalDateTime;

public record ChatMensajeVista(String rol, String contenido, LocalDateTime fecha) {
}
