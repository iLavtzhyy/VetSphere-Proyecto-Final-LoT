package com.VetSphere.Backend.modules.clinic.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
@Entity
public class OrdenLaboratorio {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private LocalDateTime fecha;
    private String tipoPrueba;
    @Column(length = 1600)
    private String resultado;
    private String estado;
    private String archivoResultadoUrl;
    @Builder.Default
    private java.math.BigDecimal precio = java.math.BigDecimal.valueOf(35.00);
    private Long facturaId;
    @ManyToOne private Mascota mascota;
    @ManyToOne private Usuario veterinarioSolicitante;
}

