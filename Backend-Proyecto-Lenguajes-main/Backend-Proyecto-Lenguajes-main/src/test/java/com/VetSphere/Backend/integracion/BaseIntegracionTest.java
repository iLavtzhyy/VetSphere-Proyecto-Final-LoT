package com.VetSphere.Backend.integracion;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Base de todas las pruebas de integracion: un UNICO contexto de Spring para
 * toda la suite (mismas anotaciones y mismos mocks en cada clase), con perfiles
 * local+test (H2 en memoria, sin correo ni IA reales).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles({"local", "test"})
public abstract class BaseIntegracionTest {

    protected static final String CLAVE_DEMO = "Demo123!";
    protected static final String ADMIN = "admin1@vetsphere.com";
    protected static final String RECEPCIONISTA = "ana.molina@vetsphere.com";
    protected static final String CLIENTE = "martin.alvarez@vetsphere.com";

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    /** El correo nunca se envia de verdad en las pruebas. */
    @MockBean
    protected JavaMailSender correoSender;

    @BeforeEach
    void limpiarInteracciones() {
        if (correoSender != null) {
            org.mockito.Mockito.reset(correoSender);
        }
    }

    /** Obtiene el token JWT de un usuario sembrado (o registrado) por el seeder. */
    protected String tokenDe(String email, String clave) throws Exception {
        MvcResult resultado = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("email", email, "password", clave))))
                .andExpect(status().isOk())
                .andReturn();
        return cuerpo(resultado).path("data").path("token").asText();
    }

    protected JsonNode cuerpo(MvcResult resultado) throws Exception {
        return objectMapper.readTree(resultado.getResponse().getContentAsString());
    }
}
