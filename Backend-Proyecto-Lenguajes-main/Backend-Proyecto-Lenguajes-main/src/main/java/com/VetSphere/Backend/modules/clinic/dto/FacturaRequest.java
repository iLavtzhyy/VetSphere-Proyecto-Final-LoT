package com.VetSphere.Backend.modules.clinic.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Payload de creacion de factura.
 *
 * NOTA: subtotal/impuestos/total son solo "sugeridos" por el cliente; el backend
 * vuelve a calcularlos (ver FacturacionService) para evitar manipulacion de precios.
 *
 * Cuando un item incluye {@code productoCodigo}, el backend IGNORA el precio
 * enviado por el cliente y usa el precio real del catalogo: es la unica forma de
 * que un cliente no pueda comprar un producto de 200 USD por 0.01 USD.
 */
public record FacturaRequest(
    String numero,
    LocalDateTime fecha,
    @NotBlank(message = "El concepto de la factura es obligatorio") @Size(max = 500) String concepto,
    BigDecimal subtotal,
    BigDecimal impuestos,
    BigDecimal total,
    String estadoPago,
    String metodoPago,
    IdRequest cliente,
    IdRequest mascota,
    List<Long> consultaIds,
    List<Long> cirugiaIds,
    List<Long> laboratorioIds,
    List<Long> hospitalizacionIds,
    List<ItemFactura> items,
    /** Codigo del cupon de descuento a aplicar (validado y calculado en el servidor). */
    @Size(max = 32) String cuponCodigo,
    @Size(max = 600) String observaciones
) {
    public record IdRequest(Long id) {}

    /**
     * Linea de factura. Si {@code productoCodigo} viene informado, el precio y
     * el descuento se resuelven contra el catalogo; en caso contrario se usa el
     * precio indicado (conceptos libres sin producto de farmacia).
     */
    public record ItemFactura(
            @Size(max = 300) String descripcion,
            Integer cantidad,
            @Positive BigDecimal precioUnitario,
            @Size(max = 60) String productoCodigo,
            @Size(max = 80) String categoria
    ) {
        /** Constructor de compatibilidad con las lineas libres (sin producto de catalogo). */
        public ItemFactura(String descripcion, Integer cantidad, BigDecimal precioUnitario) {
            this(descripcion, cantidad, precioUnitario, null, null);
        }
    }
}
