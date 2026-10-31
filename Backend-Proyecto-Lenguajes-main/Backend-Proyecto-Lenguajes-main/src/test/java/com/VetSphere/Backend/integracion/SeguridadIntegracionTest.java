package com.VetSphere.Backend.integracion;

import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Reglas de autorizacion de la cadena de seguridad: 401 sin sesion,
 * 403 sin rol adecuado y envoltorio ApiResponse tambien en errores.
 */
class SeguridadIntegracionTest extends BaseIntegracionTest {

    @Test
    void sinTokenDevuelve401ConEnvoltorioApi() throws Exception {
        mockMvc.perform(get("/api/mascotas"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.ok").value(false))
                .andExpect(jsonPath("$.mensaje").isNotEmpty());
    }

    @Test
    void clienteNoPuedeConsultarUsuariosAdministrativos() throws Exception {
        String token = tokenDe(CLIENTE, CLAVE_DEMO);

        mockMvc.perform(get("/api/admin/usuarios").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.ok").value(false));
    }

    @Test
    void clienteNoPuedeEmitirFacturas() throws Exception {
        String token = tokenDe(CLIENTE, CLAVE_DEMO);

        mockMvc.perform(post("/api/facturacion")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content("{\"concepto\":\"Intento\",\"subtotal\":10}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void clienteNoPuedeModificarMascotas() throws Exception {
        String token = tokenDe(CLIENTE, CLAVE_DEMO);

        mockMvc.perform(post("/api/mascotas")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content("{\"nombre\":\"Hacker\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void documentacionOpenApiNoEsPublica() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void laMatrizDePermisosSoloLaConsultaElAdministrador() throws Exception {
        String tokenAdmin = tokenDe(ADMIN, CLAVE_DEMO);
        String tokenRecepcion = tokenDe(RECEPCIONISTA, CLAVE_DEMO);

        mockMvc.perform(get("/api/permisos").header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ok").value(true))
                .andExpect(jsonPath("$.data.ROLE_ADMIN").isArray())
                .andExpect(jsonPath("$.data.ROLE_CLIENTE").isArray());

        mockMvc.perform(get("/api/permisos").header("Authorization", "Bearer " + tokenRecepcion))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.ok").value(false));
    }

    @Test
    void recepcionistaConsultaRotacionYCupones() throws Exception {
        String token = tokenDe(RECEPCIONISTA, CLAVE_DEMO);

        mockMvc.perform(get("/api/farmacia/rotacion").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/farmacia/cupones").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
        // ...pero no administra los cupones (crear es exclusivo del administrador).
        mockMvc.perform(post("/api/farmacia/cupones")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content("{\"codigo\":\"INTENTO\",\"tipoDescuento\":\"PORCENTAJE\",\"valor\":10}"))
                .andExpect(status().isForbidden());
    }
}
