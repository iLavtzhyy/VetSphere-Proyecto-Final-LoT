package com.VetSphere.Backend.modules.clinic.domain;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Canje de un cupon sobre una factura concreta (trazabilidad del descuento). */
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
@Entity
public class CanjeCupon {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne
    @JoinColumn(name = "cupon_id")
    private Cupon cupon;
    @ManyToOne
    @JoinColumn(name = "factura_id")
    private Factura factura;
    @Column(nullable = false, length = 32)
    private String codigo;
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal descuentoAplicado = BigDecimal.ZERO;
    @Column(nullable = false)
    private LocalDateTime fecha = LocalDateTime.now();
    @Column(length = 150)
    private String usuarioEmail;
}
