package com.VetSphere.Backend.modules.clinic.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Payload de creacion/actualizacion de un cupon de descuento.
 *
 * El alcance puede acotarse a un producto (productoCodigo) o a una categoria;
 * si no se indica ninguno, el cupon aplica a cualquier producto de la factura.
 */
public record CuponRequest(
        @Size(max = 32, message = "El codigo no puede superar los 32 caracteres") String codigo,
        @Size(max = 300, message = "La descripcion es demasiado extensa") String descripcion,
        @NotBlank(message = "El tipo de descuento es obligatorio") String tipoDescuento,
        @NotNull(message = "El valor del descuento es obligatorio")
        @DecimalMin(value = "0.00", message = "El valor del descuento no puede ser negativo") BigDecimal valor,
        @Size(max = 60) String productoCodigo,
        @Size(max = 80) String categoria,
        @DecimalMin(value = "0.00", message = "La compra minima no puede ser negativa") BigDecimal minimoSubtotal,
        LocalDateTime fechaInicio,
        LocalDateTime fechaFin,
        Integer usosMaximos,
        Boolean activo
) {
}
