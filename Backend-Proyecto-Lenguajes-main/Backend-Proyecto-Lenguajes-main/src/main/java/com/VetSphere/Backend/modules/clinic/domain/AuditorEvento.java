package com.VetSphere.Backend.modules.clinic.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Evento real de auditoria persistido en base de datos.
 * Reemplaza los eventos inventados que devolvia AuditoriaController.
 */
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
@Entity
public class AuditorEvento {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private LocalDateTime fecha;
    private String usuarioEmail;
    private String accion;
    private String entidad;
    private Long entidadId;
    @Column(length = 2000)
    private String detalle;
}
