package com.VetSphere.Backend.modules.clinic.dto;

import java.math.BigDecimal;

/**
 * Linea facturable ya normalizada por el servidor: producto resuelto, precio
 * unitario real y descuento permanente del producto aplicado.
 *
 * Se usa para calcular el alcance de un cupon (producto, categoria o global)
 * sin depender de lo que envie el cliente.
 */
public record LineaFacturable(
        String productoCodigo,
        String descripcion,
        String categoria,
        Integer cantidad,
        BigDecimal precioUnitario,
        BigDecimal descuento,
        BigDecimal subtotal
) {
    public BigDecimal importe() {
        return subtotal == null ? BigDecimal.ZERO : subtotal;
    }
}
