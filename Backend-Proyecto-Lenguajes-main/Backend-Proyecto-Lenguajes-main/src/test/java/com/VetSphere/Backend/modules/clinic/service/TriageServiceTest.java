package com.VetSphere.Backend.modules.clinic.service;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Reglas del triage veterinario: cada nivel se decide por los signos y las
 * constantes que llegan, no por un valor fijo.
 */
class TriageServiceTest {

    private final TriageService triage = new TriageService();

    private Map<String, Object> signos(Object... pares) {
        Map<String, Object> mapa = new HashMap<>();
        for (int i = 0; i < pares.length; i += 2) {
            mapa.put(String.valueOf(pares[i]), pares[i + 1]);
        }
        return mapa;
    }

    @Test
    void sinSignosElPacienteQuedaVerde() {
        TriageService.Resultado resultado = triage.clasificar(signos());

        assertEquals(TriageService.Nivel.VERDE, resultado.nivel());
        assertTrue(resultado.motivos().get(0).contains("No se detectaron signos de alarma"));
    }

    @Test
    void convulsionesClasificanEnRojo() {
        TriageService.Resultado resultado = triage.clasificar(signos("convulsiones", true));

        assertEquals(TriageService.Nivel.ROJO, resultado.nivel());
        assertTrue(resultado.siguientePaso().contains("inmediata"));
    }

    @Test
    void temperaturaCriticaClasificaEnRojo() {
        TriageService.Resultado resultado = triage.clasificar(signos("temperatura", 42.5));

        assertEquals(TriageService.Nivel.ROJO, resultado.nivel());
        assertTrue(resultado.motivos().stream().anyMatch(m -> m.contains("Temperatura")));
    }

    @Test
    void temperaturaAlteradaClasificaEnAmarillo() {
        TriageService.Resultado resultado = triage.clasificar(signos("temperatura", 39.8));

        assertEquals(TriageService.Nivel.AMARILLO, resultado.nivel());
    }

    @Test
    void traumaSinSignosCriticosClasificaEnNaranja() {
        TriageService.Resultado resultado = triage.clasificar(signos("trauma", true));

        assertEquals(TriageService.Nivel.NARANJA, resultado.nivel());
    }

    @Test
    void lasLlavesSeNormalizanSinTildesNiMayusculas() {
        TriageService.Resultado resultado = triage.clasificar(signos("Frecuencia Respiratoria", 60));

        assertEquals(TriageService.Nivel.ROJO, resultado.nivel());
        assertTrue(resultado.motivos().stream().anyMatch(m -> m.contains("respiratoria")));
    }

    @Test
    void losSignosEnTextoTambienSeAceptan() {
        TriageService.Resultado resultado = triage.clasificar(signos("convulsiones", "Si"));

        assertEquals(TriageService.Nivel.ROJO, resultado.nivel());
    }

    @Test
    void elDetalleExponeNivelEtiquetaYMotivos() {
        Map<String, Object> detalle = triage.detalle(signos("mucosasPalidas", true));

        assertEquals("ROJO", detalle.get("nivel"));
        assertEquals("Rojo", detalle.get("etiqueta"));
        assertTrue(detalle.get("descripcion").toString().contains("Emergencia"));
        assertTrue(detalle.get("motivos") instanceof java.util.List<?>);
        assertTrue(detalle.containsKey("siguientePaso"));
    }
}
