package com.VetSphere.Backend.modules.clinic.domain;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Producto de la farmacia. Los campos de descuento, lote y vencimiento son
 * opcionales (NULL) para no romper el esquema de bases ya creadas.
 */
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
@Entity
public class Producto {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true)
    private String codigo;
    private String nombre;
    private String categoria;
    private Integer stock;
    private Integer stockCritico;
    private BigDecimal precio;
    private boolean prioridadAlta;

    /** Descuento permanente en porcentaje aplicado al precio (NULL = 0). */
    private BigDecimal descuento;

    /** Producto retirado de la operacion pero conservado en el historico (NULL = activo). */
    private Boolean activo;

    /** Lote del medicamento, para trazabilidad deExpiry. */
    private String lote;

    /** Fecha de vencimiento del lote (NULL = producto sin vencimiento). */
    private LocalDate fechaVencimiento;

    /** true si el producto sigue vigente para la venta. */
    public boolean disponible() {
        return activo == null || activo;
    }

    /** Descuento efectivo en porcentaje (0 si no tiene). */
    public BigDecimal descuentoEfectivo() {
        return descuento == null ? BigDecimal.ZERO : descuento;
    }
}
