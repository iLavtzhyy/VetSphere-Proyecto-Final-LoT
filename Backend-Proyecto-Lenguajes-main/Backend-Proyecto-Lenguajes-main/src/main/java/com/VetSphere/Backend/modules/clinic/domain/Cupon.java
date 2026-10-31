package com.VetSphere.Backend.modules.clinic.domain;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Cupon de descuento. Puede aplicarse a un producto concreto, a una categoria
 * completa o a cualquier producto de la farmacia (si no se define alcance).
 */
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
@Entity
public class Cupon {
    /** Descuento porcentual sobre el importe de referencia. */
    public static final String PORCENTAJE = "PORCENTAJE";
    /** Descuento de monto fijo sobre el importe de referencia. */
    public static final String VALOR_FIJO = "VALOR_FIJO";
    /** Cupon creado manualmente por un administrador. */
    public static final String ORIGEN_MANUAL = "MANUAL";
    /** Cupon generado automaticamente por el analisis de baja rotacion. */
    public static final String ORIGEN_ROTACION = "BAJA_ROTACION";

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true, length = 32)
    private String codigo;
    @Column(length = 300)
    private String descripcion;
    @Column(nullable = false, length = 20)
    private String tipoDescuento = PORCENTAJE;
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal valor = BigDecimal.ZERO;
    /** Producto objetivo (NULL = aplica a la categoria o a todo el catalogo). */
    @ManyToOne
    @JoinColumn(name = "producto_id")
    private Producto producto;
    /** Categoria objetivo (NULL = aplica a todo el catalogo). */
    @Column(length = 80)
    private String categoria;
    /** Compra minima para que el cupon sea aplicable (NULL = sin minimo). */
    @Column(precision = 12, scale = 2)
    private BigDecimal minimoSubtotal;
    @Column(nullable = false)
    private LocalDateTime fechaInicio;
    @Column(nullable = false)
    private LocalDateTime fechaFin;
    /** Numero maximo de canjes (NULL = ilimitado). */
    private Integer usosMaximos;
    @Column(nullable = false)
    private Integer usosActuales = 0;
    @Column(nullable = false)
    private Boolean activo = true;
    @Column(nullable = false, length = 30)
    private String origen = ORIGEN_MANUAL;
    @Column(length = 150)
    private String creadoPor;
    @Column(nullable = false)
    private LocalDateTime fechaCreacion = LocalDateTime.now();
}
