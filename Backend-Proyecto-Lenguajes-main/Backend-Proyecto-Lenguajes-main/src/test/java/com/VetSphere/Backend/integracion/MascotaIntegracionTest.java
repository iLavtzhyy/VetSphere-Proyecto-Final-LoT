package com.VetSphere.Backend.integracion;

import com.VetSphere.Backend.modules.clinic.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * CRUD de mascotas con el mismo contrato del frontend: multipart para crear,
 * JSON para actualizar y paginacion opcional en el listado.
 */
@Transactional
class MascotaIntegracionTest extends BaseIntegracionTest {

    @Autowired
    private UsuarioRepository usuarioRepository;

    private MockMultipartFile parteMascota(String json) {
        return new MockMultipartFile("mascota", "mascota.json", MediaType.APPLICATION_JSON_VALUE,
                json.getBytes(StandardCharsets.UTF_8));
    }

    private long idMartin() {
        return usuarioRepository.findByEmail(CLIENTE).orElseThrow().getId();
    }

    @Test
    void crearListarYEliminarMascota() throws Exception {
        String token = tokenDe(ADMIN, CLAVE_DEMO);
        String json = "{\"nombre\":\"Toby\",\"especie\":\"Perro\",\"raza\":\"Mestizo\","
                + "\"propietarioId\":" + idMartin() + "}";

        MvcResult creacion = mockMvc.perform(multipart("/api/mascotas")
                        .file(parteMascota(json))
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ok").value(true))
                .andExpect(jsonPath("$.data.nombre").value("Toby"))
                .andReturn();
        long id = cuerpo(creacion).path("data").path("id").asLong();

        mockMvc.perform(get("/api/mascotas").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());

        mockMvc.perform(delete("/api/mascotas/" + id).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ok").value(true));
    }

    @Test
    void crearMascotaSinNombreDevuelve400() throws Exception {
        String token = tokenDe(ADMIN, CLAVE_DEMO);

        mockMvc.perform(multipart("/api/mascotas")
                        .file(parteMascota("{\"especie\":\"Gato\",\"propietarioId\":" + idMartin() + "}"))
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.ok").value(false))
                .andExpect(jsonPath("$.mensaje").value(org.hamcrest.Matchers.containsString("nombre")));
    }

    @Test
    void crearMascotaSinPropietarioDevuelve400() throws Exception {
        String token = tokenDe(ADMIN, CLAVE_DEMO);

        mockMvc.perform(multipart("/api/mascotas")
                        .file(parteMascota("{\"nombre\":\"Sin dueno\"}"))
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.ok").value(false));
    }

    @Test
    void actualizarMascotaPorJson() throws Exception {
        String token = tokenDe(ADMIN, CLAVE_DEMO);
        MvcResult creacion = mockMvc.perform(multipart("/api/mascotas")
                        .file(parteMascota("{\"nombre\":\"Rex\",\"especie\":\"Perro\",\"propietarioId\":" + idMartin() + "}"))
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        long id = cuerpo(creacion).path("data").path("id").asLong();

        mockMvc.perform(put("/api/mascotas/" + id)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Rex II\",\"especie\":\"Perro\",\"estado\":\"FALLECIDA\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.nombre").value("Rex II"))
                .andExpect(jsonPath("$.data.estado").value("FALLECIDA"));
    }

    @Test
    void listadoConPageSizeDevuelvePaginaYSinEllosArreglo() throws Exception {
        String token = tokenDe(ADMIN, CLAVE_DEMO);

        // Sin page/size: el contrato historico (arreglo plano dentro de data)
        mockMvc.perform(get("/api/mascotas").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());

        // Con page/size: envoltorio de paginacion opcional
        mockMvc.perform(get("/api/mascotas?page=0&size=1").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.contenido").isArray())
                .andExpect(jsonPath("$.data.pagina").value(0))
                .andExpect(jsonPath("$.data.tamano").value(1))
                .andExpect(jsonPath("$.data.totalElementos").isNumber());
    }

    @Test
    void historialClinicoDeUnaMascota() throws Exception {
        String token = tokenDe(ADMIN, CLAVE_DEMO);
        MvcResult creacion = mockMvc.perform(multipart("/api/mascotas")
                        .file(parteMascota("{\"nombre\":\"Mimi\",\"especie\":\"Gato\",\"propietarioId\":" + idMartin() + "}"))
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        long id = cuerpo(creacion).path("data").path("id").asLong();

        String historialJson = objectMapper.writeValueAsString(Map.of(
                "tipoEvento", "CONSULTA",
                "motivo", "Control general",
                "diagnostico", "En buen estado",
                "tratamiento", "Ninguno"));
        MockMultipartFile parteHistorial = new MockMultipartFile("historial", "historial.json",
                MediaType.APPLICATION_JSON_VALUE, historialJson.getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(multipart("/api/mascotas/" + id + "/historial")
                        .file(parteHistorial)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.tipoEvento").value("CONSULTA"));

        mockMvc.perform(get("/api/mascotas/" + id + "/historial").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());
    }
}
