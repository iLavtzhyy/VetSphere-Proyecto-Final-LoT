package com.VetSphere.Backend.modules.clinic.domain;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Medicamento retirado de farmacia y cargado a la cuenta de una hospitalizacion. */
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
@Entity
public class ConsumoHospitalizacion {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(optional = false)
    @JoinColumn(name = "hospitalizacion_id", nullable = false)
    private Hospitalizacion hospitalizacion;
    private String productoCodigo;
    private String productoNombre;
    @Column(nullable = false)
    private Integer cantidad;
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal precioUnitario;
    @Column(nullable = false)
    private LocalDateTime fecha;
}
