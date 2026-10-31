package com.VetSphere.Backend.modules.clinic.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
@Entity
@Table(name = "chat_mensajes", indexes = @Index(name = "ix_chat_mensaje_conversacion_fecha", columnList = "conversacion_id, fecha"))
public class ChatMensaje {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "conversacion_id", nullable = false)
    private ChatConversacion conversacion;

    @Column(nullable = false, length = 12)
    private String rol;

    @Lob
    @Column(nullable = false, columnDefinition = "nvarchar(max)")
    private String contenido;

    @Column(nullable = false)
    private LocalDateTime fecha;
}
