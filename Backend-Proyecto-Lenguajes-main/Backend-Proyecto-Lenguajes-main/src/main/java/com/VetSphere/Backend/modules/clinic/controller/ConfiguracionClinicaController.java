package com.VetSphere.Backend.modules.clinic.controller;

import com.VetSphere.Backend.shared.response.ApiResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Configuracion de la clinica. Los datos salen de `application.properties`
 * (`app.clinica.*`), de modo que cada instalacion define los suyos por
 * variables de entorno en lugar de tenerlos escritos en el codigo.
 */
@RestController
@RequestMapping("/api/configuracion-clinica")
public class ConfiguracionClinicaController {

    private final String nombre;
    private final String ruc;
    private final String direccion;
    private final String telefono;
    private final String email;
    private final String web;
    private final String licencia;
    private final String moneda;

    public ConfiguracionClinicaController(
            @Value("${app.clinica.nombre:VetSphere}") String nombre,
            @Value("${app.clinica.ruc:}") String ruc,
            @Value("${app.clinica.direccion:}") String direccion,
            @Value("${app.clinica.telefono:}") String telefono,
            @Value("${app.clinica.email:}") String email,
            @Value("${app.clinica.web:}") String web,
            @Value("${app.clinica.licencia:}") String licencia,
            @Value("${app.moneda:USD}") String moneda) {
        this.nombre = nombre;
        this.ruc = ruc;
        this.direccion = direccion;
        this.telefono = telefono;
        this.email = email;
        this.web = web;
        this.licencia = licencia;
        this.moneda = moneda;
    }

    @GetMapping
    public ApiResponse<Map<String, Object>> obtenerConfiguracion() {
        Map<String, Object> configuracion = new LinkedHashMap<>();
        configuracion.put("nombre", nombre);
        configuracion.put("ruc", ruc);
        configuracion.put("direccion", direccion);
        configuracion.put("telefono", telefono);
        configuracion.put("email", email);
        configuracion.put("web", web);
        configuracion.put("licencia", licencia);
        configuracion.put("moneda", moneda);
        configuracion.put("horario", "08:00 - 20:00");
        configuracion.put("requiereConsentimientoCirugia", true);
        return ApiResponse.ok("Configuracion clinica", configuracion);
    }
}
