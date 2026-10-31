package com.VetSphere.Backend.modules.clinic.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Parametros de la generacion automatica de cupones para productos de baja
 * rotacion. El descuento real de cada cupon se calcula a partir de los dias
 * que el producto lleva sin venderse (ver InventarioService).
 */
public record CuponAutoRequest(
        @Min(value = 1, message = "El umbral debe ser de al menos 1 dia")
        @Max(value = 365, message = "El umbral no puede superar 365 dias") Integer diasSinVenta,
        @DecimalMin(value = "0.00", message = "El descuento no puede ser negativo")
        @DecimalMax(value = "80.00", message = "El descuento no puede superar el 80%")
        BigDecimal descuentoPorcentaje,
        @Min(value = 1, message = "La vigencia debe ser de al menos 1 dia")
        @Max(value = 365, message = "La vigencia no puede superar 365 dias") Integer duracionDias,
        @Size(max = 200) String descripcion
) {
}
