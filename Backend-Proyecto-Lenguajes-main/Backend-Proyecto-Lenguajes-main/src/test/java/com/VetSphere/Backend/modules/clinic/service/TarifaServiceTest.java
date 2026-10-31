package com.VetSphere.Backend.modules.clinic.service;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Pruebas unitarias de la unica fuente de verdad de precios.
 */
class TarifaServiceTest {

    private final TarifaService tarifaService = new TarifaService(
            new BigDecimal("45.00"), new BigDecimal("280.00"), new BigDecimal("35.00"),
            new BigDecimal("45.00"), new BigDecimal("120.00"), new BigDecimal("35.00"),
            new BigDecimal("65.00"));

    @Test
    void precioPorTipoDevuelveLaTarifaCorrespondiente() {
        assertEquals(new BigDecimal("120.00"), tarifaService.precioPorTipo("EMERGENCIA"));
        assertEquals(new BigDecimal("35.00"), tarifaService.precioPorTipo("VACUNA"));
        assertEquals(new BigDecimal("280.00"), tarifaService.precioPorTipo("CIRUGIA"));
        assertEquals(new BigDecimal("65.00"), tarifaService.precioPorTipo("CONTROL"));
    }

    @Test
    void tipoDesconocidoOVacioUsaLaTarifaDeConsulta() {
        assertEquals(new BigDecimal("45.00"), tarifaService.precioPorTipo("CUALQUIERA"));
        assertEquals(new BigDecimal("45.00"), tarifaService.precioPorTipo(null));
        assertEquals(new BigDecimal("45.00"), tarifaService.precioPorTipo("  "));
    }

    @Test
    void preciosBaseSeExponePorMetodoEspecifico() {
        assertEquals(new BigDecimal("45.00"), tarifaService.precioConsulta());
        assertEquals(new BigDecimal("280.00"), tarifaService.precioCirugia());
        assertEquals(new BigDecimal("35.00"), tarifaService.precioLaboratorio());
        assertEquals(new BigDecimal("45.00"), tarifaService.precioHospitalizacion());
    }
}
