package com.VetSphere.Backend.security;

import org.springframework.http.HttpMethod;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Unico punto de verdad sobre el acceso a cada modulo del sistema.
 *
 * <p>La tabla {@link #REGLAS} se usa para dos cosas:</p>
 * <ol>
 *   <li>construir la cadena de seguridad ({@code SecurityConfig}),</li>
 *   <li>exponer la matriz de permisos por rol ({@code PermisoController}).</li>
 * </ol>
 *
 * <p>Antes cada una de las dos cosas estaba escrita por separado y ya se
 * habian desincronizado (la interfaz ofrecia acciones que el backend
 * rechazaba). Al derivar ambas de la misma tabla la divergencia es
 * imposible por construccion.</p>
 *
 * <p>El orden importa: en Spring Security gana la PRIMERA regla que
 * coincide, por lo que las reglas mas especificas van antes que las
 * generales.</p>
 */
public final class ReglasSeguridad {

    /** Forma de acceso de una regla. */
    public enum Modo {
        /** Sin necesidad de sesion. */
        PUBLICO,
        /** Cualquier usuario autenticado. */
        AUTENTICADO,
        /** Solo los roles indicados (se anteponen con el prefijo ROLE_). */
        ROLES
    }

    /**
     * Una regla de acceso.
     *
     * @param modulo   nombre del modulo (se muestra en la matriz de permisos);
     *                 {@code null} para las rutas publicas, que no se listan.
     * @param modo     forma de acceso.
     * @param roles    roles con acceso, sin el prefijo ROLE_.
     * @param metodos  metodos HTTP a los que aplica; vacio significa todos.
     * @param patrones patrones de ruta (ant).
     */
    public record Regla(String modulo, Modo modo, Set<String> roles,
                         List<HttpMethod> metodos, List<String> patrones) {

        public Regla {
            roles = Set.copyOf(roles);
            metodos = List.copyOf(metodos);
            patrones = List.copyOf(patrones);
        }
    }

    /** Roles del sistema, sin el prefijo ROLE_. */
    public static final List<String> ROLES = List.of("ADMIN", "VETERINARIO", "RECEPCIONISTA", "CLIENTE");

    private static final Set<String> ADMIN = Set.of("ADMIN");
    private static final Set<String> PERSONAL = Set.of("ADMIN", "VETERINARIO", "RECEPCIONISTA");
    private static final Set<String> CLINICA = Set.of("ADMIN", "VETERINARIO");
    private static final Set<String> TODOS = Set.of("ADMIN", "VETERINARIO", "RECEPCIONISTA", "CLIENTE");

    /** Tabla de reglas: se evalua en orden y gana la primera que coincide. */
    public static final List<Regla> REGLAS = List.of(
            // ---- Publicos -------------------------------------------------
            new Regla("usuarios", Modo.ROLES, ADMIN, List.of(HttpMethod.POST),
                    List.of("/api/auth/desactivar-cuenta", "/api/auth/confirmar-desactivacion")),
            new Regla(null, Modo.PUBLICO, Set.of(), List.of(),
                    List.of("/api/auth/**", "/api/veterinarios/publico", "/soap/**", "/error")),
            // ---- Documentacion de la API: solo administracion --------------
            new Regla("documentacion-api", Modo.ROLES, ADMIN, List.of(),
                    List.of("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html")),
            // ---- Integraciones (proxy SOAP) -------------------------------
            new Regla("integraciones", Modo.ROLES, ADMIN, List.of(),
                    List.of("/api/integraciones/**")),
            // ---- Reportes ------------------------------------------------
            // El historial en PDF lo descarga el personal y el propio cliente.
            new Regla("reportes-mascotas", Modo.ROLES, TODOS, List.of(),
                    List.of("/api/reportes/mascotas/*/historial.pdf")),
            new Regla("reportes-finanzas", Modo.ROLES, Set.of("ADMIN", "RECEPCIONISTA"), List.of(),
                    List.of("/api/reportes/finanzas.xlsx")),
            new Regla("reportes-mascotas", Modo.ROLES, PERSONAL, List.of(),
                    List.of("/api/reportes/mascotas-csv")),
            new Regla("usuarios", Modo.ROLES, ADMIN, List.of(),
                    List.of("/api/admin/**")),
            new Regla("reportes", Modo.ROLES, ADMIN, List.of(),
                    List.of("/api/reportes/**")),
            // ---- Ficha del equipo veterinario ----------------------------
            // Todo el personal puede leerla; solo la administracion la modifica.
            new Regla("equipo-veterinario", Modo.ROLES, PERSONAL, List.of(HttpMethod.GET),
                    List.of("/api/equipo-veterinario", "/api/equipo-veterinario/**")),
            new Regla("equipo-veterinario", Modo.ROLES, ADMIN, List.of(),
                    List.of("/api/equipo-veterinario", "/api/equipo-veterinario/**")),
            // ---- Facturacion (incluye la lectura del PDF) -----------------
            new Regla("facturacion", Modo.ROLES, Set.of("ADMIN", "RECEPCIONISTA"), List.of(),
                    List.of("/api/facturacion", "/api/facturacion/**", "/api/facturas/**")),
            // ---- Mascotas: lectura amplia, escritura restringida ----------
            new Regla("mascotas", Modo.ROLES, TODOS, List.of(HttpMethod.GET),
                    List.of("/api/mascotas", "/api/mascotas/**")),
            new Regla("mascotas", Modo.ROLES, PERSONAL, List.of(),
                    List.of("/api/mascotas", "/api/mascotas/**")),
            // ---- Clientes y recepcion -------------------------------------
            new Regla("clientes", Modo.ROLES, Set.of("ADMIN", "RECEPCIONISTA"), List.of(),
                    List.of("/api/clientes", "/api/clientes/**")),
            new Regla("recepcion", Modo.ROLES, Set.of("ADMIN", "RECEPCIONISTA"), List.of(),
                    List.of("/api/recepcion", "/api/recepcion/**")),
            // ---- Citas ----------------------------------------------------
            new Regla("citas", Modo.ROLES, PERSONAL, List.of(),
                    List.of("/api/citas", "/api/citas/**")),
            // ---- Atencion clinica ------------------------------------------
            new Regla("consultas", Modo.ROLES, CLINICA, List.of(),
                    List.of("/api/consultas", "/api/consultas/**")),
            new Regla("cirugias", Modo.ROLES, CLINICA, List.of(),
                    List.of("/api/cirugias", "/api/cirugias/**")),
            new Regla("laboratorio", Modo.ROLES, CLINICA, List.of(),
                    List.of("/api/laboratorio", "/api/laboratorio/**")),
            new Regla("hospitalizaciones", Modo.ROLES, CLINICA, List.of(),
                    List.of("/api/hospitalizaciones", "/api/hospitalizaciones/**")),
            // ---- Farmacia --------------------------------------------------
            // Las promociones las administra el administrador y el analisis de
            // rotacion lo consulta el personal de farmacia.
            new Regla("cupones", Modo.ROLES, Set.of("ADMIN", "RECEPCIONISTA"), List.of(HttpMethod.GET),
                    List.of("/api/farmacia/cupones", "/api/farmacia/cupones/**")),
            new Regla("cupones", Modo.ROLES, Set.of("ADMIN", "RECEPCIONISTA"), List.of(HttpMethod.POST),
                    List.of("/api/farmacia/cupones/validar")),
            new Regla("cupones", Modo.ROLES, ADMIN, List.of(HttpMethod.POST),
                    List.of("/api/farmacia/cupones/auto")),
            new Regla("cupones", Modo.ROLES, ADMIN, List.of(),
                    List.of("/api/farmacia/cupones/**")),
            new Regla("rotacion", Modo.ROLES, Set.of("ADMIN", "RECEPCIONISTA"), List.of(),
                    List.of("/api/farmacia/rotacion")),
            new Regla("farmacia", Modo.ROLES, Set.of("ADMIN", "RECEPCIONISTA"), List.of(),
                    List.of("/api/farmacia", "/api/farmacia/**", "/api/productos", "/api/productos/**")),
            // ---- Roles y permisos: solo administracion ----------------------
            new Regla("permisos", Modo.ROLES, ADMIN, List.of(),
                    List.of("/api/roles", "/api/roles/**", "/api/permisos", "/api/permisos/**")),
            // ---- Asistente de IA -------------------------------------------
            // El cliente no tiene acceso a datos de toda la clinica.
            new Regla("chat-administracion", Modo.ROLES, ADMIN, List.of(),
                    List.of("/api/chat/admin/**")),
            new Regla("chat", Modo.ROLES, TODOS, List.of(),
                    List.of("/api/chat", "/api/chat/**")),
            // ---- Cuenta propia del usuario autenticado ----------------------
            new Regla("cuenta", Modo.AUTENTICADO, Set.of(), List.of(),
                    List.of("/api/cuenta", "/api/cuenta/**")),
            // ---- Cartelera del equipo veterinario --------------------------
            // Devuelve solo la ficha profesional (no la entidad Usuario), asi
            // que puede verla tambien el cliente.
            new Regla("veterinarios", Modo.ROLES, TODOS, List.of(),
                    List.of("/api/veterinarios", "/api/veterinarios/**")),
            new Regla("dashboard", Modo.ROLES, PERSONAL, List.of(),
                    List.of("/api/dashboard", "/api/dashboard/**")),
            // ---- Movimientos de inventario: recepcion tambien puede consultarlos
            new Regla("inventario", Modo.ROLES, Set.of("ADMIN", "RECEPCIONISTA"), List.of(),
                    List.of("/api/inventario/movimientos")),
            // ---- Modulos reservados exclusivamente a administracion --------
            new Regla("auditoria", Modo.ROLES, ADMIN, List.of(), List.of("/api/auditoria/**")),
            new Regla("caja", Modo.ROLES, ADMIN, List.of(), List.of("/api/caja/**")),
            new Regla("compras", Modo.ROLES, ADMIN, List.of(), List.of("/api/compras/**")),
            new Regla("proveedores", Modo.ROLES, ADMIN, List.of(), List.of("/api/proveedores/**")),
            new Regla("turnos", Modo.ROLES, ADMIN, List.of(), List.of("/api/turnos/**")),
            new Regla("urgencias", Modo.ROLES, ADMIN, List.of(), List.of("/api/urgencias/**")),
            new Regla("vacunacion", Modo.ROLES, ADMIN, List.of(), List.of("/api/vacunacion/**")),
            new Regla("seguimientos", Modo.ROLES, ADMIN, List.of(), List.of("/api/seguimientos/**")),
            new Regla("imagenologia", Modo.ROLES, ADMIN, List.of(), List.of("/api/imagenologia/**")),
            new Regla("especialidades", Modo.ROLES, ADMIN, List.of(),
                    List.of("/api/especialidades", "/api/especialidades/**")),
            new Regla("configuracion-clinica", Modo.ROLES, ADMIN, List.of(),
                    List.of("/api/configuracion-clinica", "/api/configuracion-clinica/**")),
            new Regla("consentimientos", Modo.ROLES, ADMIN, List.of(), List.of("/api/consentimientos/**")),
            new Regla("triage", Modo.ROLES, ADMIN, List.of(), List.of("/api/triage/**")),
            new Regla("notificaciones", Modo.ROLES, ADMIN, List.of(), List.of("/api/notificaciones/**")),
            new Regla("archivos", Modo.ROLES, ADMIN, List.of(), List.of("/api/archivos/**")),
            new Regla("inventario", Modo.ROLES, ADMIN, List.of(), List.of("/api/inventario")),
            new Regla("estadisticas", Modo.ROLES, ADMIN, List.of(), List.of("/api/estadisticas/**")),
            new Regla("administracion", Modo.ROLES, ADMIN, List.of(), List.of("/api/administracion/**")),
            // ---- Archivos subidos: los descarga el frontend con el token --
            new Regla("archivos", Modo.AUTENTICADO, Set.of(), List.of(), List.of("/uploads/**"))
    );

    private ReglasSeguridad() {
    }

    /**
     * Modulos a los que puede acceder cada rol, deducidos de la tabla de
     * reglas. Las rutas publicas no aparecen: no requieren ningun rol.
     */
    public static Map<String, List<String>> modulosPorRol() {
        Map<String, LinkedHashSet<String>> porRol = new LinkedHashMap<>();
        for (String rol : ROLES) {
            porRol.put("ROLE_" + rol, new LinkedHashSet<>());
        }
        for (Regla regla : REGLAS) {
            if (regla.modulo() == null) {
                continue;
            }
            switch (regla.modo()) {
                case ROLES -> regla.roles().forEach(rol -> porRol.get("ROLE_" + rol).add(regla.modulo()));
                case AUTENTICADO -> porRol.values().forEach(modulos -> modulos.add(regla.modulo()));
                case PUBLICO -> {
                    // Las rutas publicas no se listan en la matriz.
                }
            }
        }
        Map<String, List<String>> resultado = new LinkedHashMap<>();
        porRol.forEach((rol, modulos) -> resultado.put(rol, List.copyOf(modulos)));
        return resultado;
    }
}
