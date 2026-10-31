package com.VetSphere.Backend.modules.clinic.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Payload de creacion y actualizacion de ordenes de compra.
 *
 * El proveedor y el responsable se envian por id porque la entidad guarda las
 * relaciones ManyToOne. El numero de orden es opcional: si no se envia, el
 * servidor genera el correlativo OC-&lt;anio&gt;-&lt;consecutivo&gt;.
 */
public record OrdenCompraRequest(
        @Size(max = 32, message = "El numero de orden no puede superar los 32 caracteres")
        String numero,
        @NotNull(message = "Debes indicar el proveedor de la orden")
        Long proveedorId,
        @NotNull(message = "Debes indicar la fecha de la orden")
        LocalDate fecha,
        @Size(max = 30, message = "El estado de la orden no puede superar los 30 caracteres")
        String estado,
        @PositiveOrZero(message = "El total de la orden no puede ser negativo")
        BigDecimal total,
        Long responsableId,
        @Size(max = 1000, message = "Las observaciones no pueden superar los 1000 caracteres")
        String observaciones,
        @Size(max = 2000, message = "El detalle no puede superar los 2000 caracteres")
        String detalle
) {
}
