package com.VetSphere.Backend.modules.clinic.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Movimiento de inventario (entrada, salida, ajuste o venta) registrado
 * automaticamente por ProductoService cada vez que el stock de un producto
 * cambia.
 *
 * El campo {@code productoId} permite consultar los movimientos de un producto
 * concreto (trazabilidad) sin depender del texto descriptivo.
 */
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
@Entity
public class MovimientoInventario {
    public static final String ENTRADA = "ENTRADA";
    public static final String SALIDA = "SALIDA";
    public static final String AJUSTE = "AJUSTE";
    /** Salida de almacen generada por la venta de un producto (facturacion). */
    public static final String VENTA = "VENTA";

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private LocalDateTime fecha;
    private String producto;
    /** Identificador del producto (NULL en movimientos historicos). */
    private Long productoId;
    @Column(nullable = false, length = 20)
    private String tipo;
    private Integer cantidad;
    private Integer stockAnterior;
    private Integer stockNuevo;
    private String motivo;
    private String usuarioEmail;
}
