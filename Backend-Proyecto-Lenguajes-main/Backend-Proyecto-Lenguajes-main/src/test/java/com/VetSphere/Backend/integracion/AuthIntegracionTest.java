package com.VetSphere.Backend.integracion;

import com.VetSphere.Backend.modules.clinic.domain.CodigoVerificacion;
import com.VetSphere.Backend.modules.clinic.repository.CodigoVerificacionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Flujo completo de autenticacion: login, credenciales invalidas,
 * registro con verificacion en dos pasos.
 */
@Transactional
class AuthIntegracionTest extends BaseIntegracionTest {

    @Autowired
    private CodigoVerificacionRepository codigoRepository;

    @Test
    void loginConCredencialesValidasDevuelveToken() throws Exception {
        MvcResult resultado = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + ADMIN + "\",\"password\":\"" + CLAVE_DEMO + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ok").value(true))
                .andReturn();

        var json = cuerpo(resultado);
        assertFalse(json.path("data").path("token").asText().isBlank());
        assertEquals(ADMIN, json.path("data").path("email").asText());
        assertTrue(json.path("data").path("roles").toString().contains("ROLE_ADMIN"));
    }

    @Test
    void loginConPasswordIncorrectoDevuelve400() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + ADMIN + "\",\"password\":\"clave-mala\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.ok").value(false))
                .andExpect(jsonPath("$.mensaje").value("Credenciales incorrectas."));
    }

    @Test
    void loginSinCuerpoDevuelve400ConEnvoltorioApi() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.ok").value(false));
    }

    @Test
    void registroDeClienteSinCedulaEsRechazado() throws Exception {
        mockMvc.perform(post("/api/auth/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombres\":\"Nuevo\",\"apellidos\":\"Usuario\",\"email\":\"sin.cedula@vetsphere.test\",\"password\":\"Segura123\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.ok").value(false));
    }

    @Test
    void registroYVerificacionEnDosPasosActivanLaCuenta() throws Exception {
        String email = "nuevo.usuario@vetsphere.test";

        MvcResult registro = mockMvc.perform(post("/api/auth/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombres\":\"Nuevo\",\"apellidos\":\"Usuario\",\"email\":\"" + email
                                + "\",\"documento\":\"1830000001\",\"password\":\"Segura123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ok").value(true))
                .andReturn();
        assertEquals("", cuerpo(registro).path("data").path("token").asText(),
                "El registro NO debe entregar token hasta verificar el codigo");

        String codigo = ultimoCodigoDe(email);
        assertNotNull(codigo, "El seeder/generador debio crear un codigo de verificacion");

        mockMvc.perform(post("/api/auth/verificar-2fa")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"codigo\":\"" + codigo + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ok").value(true))
                .andExpect(jsonPath("$.data.token").isNotEmpty());
    }

    @Test
    void codigoDeVerificacionInvalidoDevuelve400() throws Exception {
        mockMvc.perform(post("/api/auth/verificar-2fa")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + ADMIN + "\",\"codigo\":\"000000\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.ok").value(false));
    }

    private String ultimoCodigoDe(String email) {
        return codigoRepository.findAll().stream()
                .filter(codigo -> email.equals(codigo.getEmail()) && !codigo.isUsado())
                .max(Comparator.comparing(CodigoVerificacion::getId))
                .map(CodigoVerificacion::getCodigo)
                .orElse(null);
    }
}
