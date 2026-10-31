package com.VetSphere.Backend.modules.clinic.dto;

import java.time.LocalDateTime;

public record ChatConversacionResumen(Long id, String usuarioEmail, String nombreUsuario,
                                      String titulo, LocalDateTime actualizadoEn, long cantidadMensajes) {
}
