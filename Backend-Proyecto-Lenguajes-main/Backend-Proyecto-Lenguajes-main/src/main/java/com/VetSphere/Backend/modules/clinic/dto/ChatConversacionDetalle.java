package com.VetSphere.Backend.modules.clinic.dto;

import java.time.LocalDateTime;
import java.util.List;

public record ChatConversacionDetalle(Long id, String usuarioEmail, String nombreUsuario,
                                      String titulo, LocalDateTime actualizadoEn,
                                      List<ChatMensajeVista> mensajes) {
}
