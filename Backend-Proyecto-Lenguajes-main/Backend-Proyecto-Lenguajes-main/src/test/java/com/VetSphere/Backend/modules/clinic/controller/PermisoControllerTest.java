package com.VetSphere.Backend.modules.clinic.controller;

import com.VetSphere.Backend.security.ReglasSeguridad;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * La matriz de permisos que se expone al frontend se deduce de la misma tabla
 * de reglas que construye la cadena de seguridad, de modo que no puede
 * contradecirla. Estos tests comprueban ademas que la matriz dice la verdad
 * sobre los roles mas delicados (cliente, recepcionista y veterinario).
 */
class PermisoControllerTest {

    private final Map<String, List<String>> permisos = PermisoController.permisosPorRol();

    @Test
    void elAdministradorTieneAccesoATodosLosModulosReservados() {
        List<String> admin = permisos.get("ROLE_ADMIN");
        assertTrue(admin.contains("farmacia"));
        assertTrue(admin.contains("cupones"));
        assertTrue(admin.contains("rotacion"));
        assertTrue(admin.contains("facturacion"));
        assertTrue(admin.contains("usuarios"));
        assertTrue(admin.contains("auditoria"));
        // Modulos que antes devolvian datos inventados.
        assertTrue(admin.contains("turnos"));
        assertTrue(admin.contains("compras"));
        assertTrue(admin.contains("urgencias"));
        assertTrue(admin.contains("vacunacion"));
        assertTrue(admin.contains("seguimientos"));
        assertTrue(admin.contains("imagenologia"));
        assertTrue(admin.contains("consentimientos"));
        assertTrue(admin.contains("triage"));
    }

    @Test
    void recepcionistaAccedeAFarmaciaYFacturacionPeroNoAdministraCupones() {
        List<String> recepcion = permisos.get("ROLE_RECEPCIONISTA");
        assertTrue(recepcion.contains("farmacia"));
        assertTrue(recepcion.contains("cupones"));
        assertTrue(recepcion.contains("rotacion"));
        assertTrue(recepcion.contains("facturacion"));
        assertTrue(recepcion.contains("citas"));
        assertTrue(recepcion.contains("clientes"));
        // La administracion de usuarios es exclusiva del administrador.
        assertFalse(recepcion.contains("usuarios"));
        assertFalse(recepcion.contains("auditoria"));
        assertFalse(recepcion.contains("turnos"));
        assertFalse(recepcion.contains("consentimientos"));
    }

    @Test
    void veterinarioNoAccedeAFarmaciaNiFacturacion() {
        List<String> veterinario = permisos.get("ROLE_VETERINARIO");
        assertTrue(veterinario.contains("consultas"));
        assertTrue(veterinario.contains("cirugias"));
        assertTrue(veterinario.contains("laboratorio"));
        assertTrue(veterinario.contains("hospitalizaciones"));
        assertFalse(veterinario.contains("farmacia"));
        assertFalse(veterinario.contains("cupones"));
        assertFalse(veterinario.contains("facturacion"));
        assertFalse(veterinario.contains("usuarios"));
    }

    @Test
    void clienteSoloVeSusPropiosModulos() {
        List<String> cliente = permisos.get("ROLE_CLIENTE");
        assertTrue(cliente.contains("mascotas"));
        assertTrue(cliente.contains("veterinarios"));
        assertTrue(cliente.contains("cuenta"));
        assertTrue(cliente.contains("reportes-mascotas"));
        assertFalse(cliente.contains("farmacia"));
        assertFalse(cliente.contains("facturacion"));
        assertFalse(cliente.contains("reportes"));
        assertFalse(cliente.contains("usuarios"));
        assertFalse(cliente.contains("citas"));
    }

    @Test
    void todosLosRolesDeclaradosTienenMatriz() {
        assertEquals(List.copyOf(ReglasSeguridad.ROLES).size(), permisos.size());
        assertTrue(permisos.containsKey("ROLE_ADMIN"));
        assertTrue(permisos.containsKey("ROLE_VETERINARIO"));
        assertTrue(permisos.containsKey("ROLE_RECEPCIONISTA"));
        assertTrue(permisos.containsKey("ROLE_CLIENTE"));
        assertTrue(permisos.keySet().stream().allMatch(rol -> !permisos.get(rol).isEmpty()));
    }

    @Test
    void cadaModuloDeLaTablaDeReglasApareceEnLaMatriz() {
        List<String> modulos = ReglasSeguridad.REGLAS.stream()
                .map(ReglasSeguridad.Regla::modulo)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .toList();
        List<String> declarados = permisos.values().stream().flatMap(List::stream).distinct().toList();
        assertTrue(declarados.containsAll(modulos),
                "La matriz no declara: " + modulos.stream().filter(m -> !declarados.contains(m)).toList());
    }
}
