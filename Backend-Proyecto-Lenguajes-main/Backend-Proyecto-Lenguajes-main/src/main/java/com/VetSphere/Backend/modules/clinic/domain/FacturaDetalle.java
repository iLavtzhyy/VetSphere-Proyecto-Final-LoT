package com.VetSphere.Backend.modules.clinic.domain;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

/**
 * Linea persistida de la factura. Permite conocer QUE se vendio (producto,
 * cantidad y precio unitario real) y no solo el concepto en texto libre.
 *
 * Es la base del analisis de rotacion de inventario: sin detalle de producto
 * no es posible saber que articulos no se venden.
 */
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
@Entity
public class FacturaDetalle {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne
    @JoinColumn(name = "factura_id")
    private Factura factura;
    /** Producto de farmacia vendido (NULL cuando la linea es un servicio clinico). */
    @ManyToOne
    @JoinColumn(name = "producto_id")
    private Producto producto;
    @Column(length = 300)
    private String descripcion;
    @Column(nullable = false)
    private Integer cantidad = 1;
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal precioUnitario = BigDecimal.ZERO;
    /** Descuento aplicado a la linea (cupon o descuento permanente del producto). */
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal descuento = BigDecimal.ZERO;
    /** Importe de la linea ya descontado (precio x cantidad - descuento). */
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotal = BigDecimal.ZERO;
}
