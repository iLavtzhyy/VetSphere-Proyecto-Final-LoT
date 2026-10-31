package com.VetSphere.Backend.integracion;

import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Exportacion de mascotas a CSV: UTF-8 con BOM para Excel y cabecera
 * Id,Nombre,Especie,Raza,Peso,Estado,Cliente.
 */
@Transactional
class ReporteCsvIntegracionTest extends BaseIntegracionTest {

    @Test
    void exportarMascotasEnCsvConCabeceraYBom() throws Exception {
        String token = tokenDe(ADMIN, CLAVE_DEMO);

        var resultado = mockMvc.perform(get("/api/reportes/mascotas-csv")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/csv"))
                .andReturn();

        String csv = new String(resultado.getResponse().getContentAsByteArray(), StandardCharsets.UTF_8);
        assertTrue(csv.startsWith("\uFEFF"), "El CSV debe empezar con BOM para que Excel abra los acentos");
        assertTrue(csv.contains("Id,Nombre,Especie"), "Falta la cabecera del CSV. Contenido: " + csv);
    }

    @Test
    void elCsvDeMascotasNoEstaDisponibleParaClientes() throws Exception {
        String token = tokenDe(CLIENTE, CLAVE_DEMO);

        mockMvc.perform(get("/api/reportes/mascotas-csv")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.ok").value(false));
    }
}
