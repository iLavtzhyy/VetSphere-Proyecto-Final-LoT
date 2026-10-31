package com.VetSphere.Backend.modules.clinic.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
@Entity
public class Hospitalizacion {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private LocalDateTime ingreso;
    private LocalDateTime alta;
    private String jaula;
    private String motivo;
    @Column(length = 1200)
    private String planCuidados;
    private String estado;
    @Builder.Default
    private java.math.BigDecimal precio = java.math.BigDecimal.valueOf(45.00);
    private Long facturaId;
    @ManyToOne private Mascota mascota;
    @ManyToOne private Usuario veterinarioResponsable;
}

