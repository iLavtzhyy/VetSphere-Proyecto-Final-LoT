package com.VetSphere.Backend.modules.clinic.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * Unica fuente de verdad de los precios de los servicios clinicos.
 * Los valores se leen de properties (app.tarifas.*) con defaults sensatos,
 * de modo que los precios ya no estan hardcodeados en controladores ni servicios.
 */
@Service
public class TarifaService {
    private final BigDecimal consulta;
    private final BigDecimal cirugia;
    private final BigDecimal laboratorio;
    private final BigDecimal hospitalizacion;
    private final BigDecimal emergencia;
    private final BigDecimal vacuna;
    private final BigDecimal control;

    public TarifaService(
            @Value("${app.tarifas.consulta:45.00}") BigDecimal consulta,
            @Value("${app.tarifas.cirugia:280.00}") BigDecimal cirugia,
            @Value("${app.tarifas.laboratorio:35.00}") BigDecimal laboratorio,
            @Value("${app.tarifas.hospitalizacion:45.00}") BigDecimal hospitalizacion,
            @Value("${app.tarifas.emergencia:120.00}") BigDecimal emergencia,
            @Value("${app.tarifas.vacuna:35.00}") BigDecimal vacuna,
            @Value("${app.tarifas.control:65.00}") BigDecimal control) {
        this.consulta = consulta;
        this.cirugia = cirugia;
        this.laboratorio = laboratorio;
        this.hospitalizacion = hospitalizacion;
        this.emergencia = emergencia;
        this.vacuna = vacuna;
        this.control = control;
    }

    public BigDecimal precioConsulta() {
        return consulta;
    }

    public BigDecimal precioCirugia() {
        return cirugia;
    }

    public BigDecimal precioLaboratorio() {
        return laboratorio;
    }

    public BigDecimal precioHospitalizacion() {
        return hospitalizacion;
    }

    /**
     * Tarifa por tipo de evento clinico (CONSULTA, EMERGENCIA, CIRUGIA, VACUNA, CONTROL...).
     */
    public BigDecimal precioPorTipo(String tipo) {
        if (tipo == null) return consulta;
        return switch (tipo.trim().toUpperCase(java.util.Locale.ROOT)) {
            case "EMERGENCIA" -> emergencia;
            case "CIRUGIA", "CIRUGÍA" -> cirugia;
            case "VACUNA", "VACUNACION" -> vacuna;
            case "CONTROL" -> control;
            case "LABORATORIO" -> laboratorio;
            case "HOSPITALIZACION" -> hospitalizacion;
            default -> consulta;
        };
    }
}
