package com.VetSphere.Backend.modules.clinic.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

/**
 * Payload para simular la aplicacion de un cupon sobre un carrito de venta.
 *
 * La simulacion usa EXACTAMENTE el mismo calculo que la emision de la factura
 * (precios del catalogo, descuentos de producto y cupon), por lo que el
 * importe que ve el personal en pantalla es el que se cobrara.
 */
public record CuponSimulacionRequest(
        @NotBlank(message = "El codigo del cupon es obligatorio")
        @Size(max = 32, message = "El codigo del cupon es demasiado largo") String codigo,
        @Size(max = 500, message = "El concepto es demasiado extenso") String concepto,
        BigDecimal subtotal,
        List<FacturaRequest.ItemFactura> items
) {
}
