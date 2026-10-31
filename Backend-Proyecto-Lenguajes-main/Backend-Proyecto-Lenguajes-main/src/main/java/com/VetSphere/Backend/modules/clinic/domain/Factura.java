package com.VetSphere.Backend.modules.clinic.domain;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
@Entity
public class Factura {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String numero;
    private LocalDateTime fecha;
    private String concepto;
    /** Base imponible: importe bruto menos descuentos aplicados. */
    private BigDecimal subtotal;
    private BigDecimal impuestos;
    private BigDecimal total;
    /** Suma de los descuentos aplicados (cupones y descuentos de producto). */
    @Column(precision = 12, scale = 2)
    private BigDecimal descuento;
    /** Codigo del cupon aplicado, si hubo. */
    @Column(length = 32)
    private String cuponCodigo;
    private String estadoPago;
    private String metodoPago;
    @Column(length = 600)
    private String observaciones;
    @ManyToOne private Usuario cliente;
    @ManyToOne private Mascota mascota;

    /** Detalle persistido de la factura (productos y servicios facturados). */
    @OneToMany(mappedBy = "factura", cascade = jakarta.persistence.CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private java.util.List<FacturaDetalle> detalles = new java.util.ArrayList<>();

    /** Descuento total aplicado (0 si no hubo). */
    public BigDecimal descuentoEfectivo() {
        return descuento == null ? BigDecimal.ZERO : descuento;
    }
}
