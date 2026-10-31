package com.VetSphere.Backend.modules.clinic.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Orden de compra dirigida a un proveedor de la clinica (persistida en base de
 * datos). Reemplaza el facade de demostracion que devolvia ordenes falsas.
 *
 * El numero de la orden lo genera el servidor con el formato OC-&lt;anio&gt;-&lt;consecutivo&gt;.
 */
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
@Entity
public class OrdenCompra {
    /** Estados validos de una orden de compra. */
    public static final String SOLICITADA = "Solicitada";
    public static final String RECIBIDA = "Recibida";
    public static final String CANCELADA = "Cancelada";
    public static final String COMPLETADA = "Completada";

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    /** Numero correlativo de la orden: OC-&lt;anio&gt;-&lt;consecutivo&gt;. */
    @Column(length = 32)
    private String numero;
    /** Proveedor al que se dirige la orden. */
    @ManyToOne
    private Proveedor proveedor;
    /** Fecha de emision de la orden. */
    private LocalDate fecha;
    /** Estado de la orden: Solicitada, Recibida, Cancelada o Completada. */
    @Column(length = 30)
    private String estado;
    /** Importe total de la orden. */
    @Column(precision = 12, scale = 2)
    private BigDecimal total;
    /** Usuario responsable de la orden (opcional). */
    @ManyToOne
    @JsonIgnoreProperties({"roles", "password"})
    private Usuario responsable;
    /** Observaciones de la orden. */
    @Column(length = 1000)
    private String observaciones;
    /** Detalle en texto libre con los productos solicitados. */
    @Column(length = 2000)
    private String detalle;
}
