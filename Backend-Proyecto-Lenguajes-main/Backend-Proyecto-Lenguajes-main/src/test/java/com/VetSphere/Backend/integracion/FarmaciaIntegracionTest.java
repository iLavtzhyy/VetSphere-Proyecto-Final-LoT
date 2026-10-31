package com.VetSphere.Backend.integracion;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Farmacia: inventario, movimientos de stock persistentes y validacion de
 * stock insuficiente (400 con envoltorio ApiResponse).
 */
@Transactional
class FarmaciaIntegracionTest extends BaseIntegracionTest {

    private String primerCodigoDeInventario(String token) throws Exception {
        var resultado = mockMvc.perform(get("/api/farmacia/inventario")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andReturn();
        var productos = cuerpo(resultado).path("data");
        assertTrue(productos.size() > 0, "El seeder debio cargar productos de farmacia");
        return productos.get(0).path("codigo").asText();
    }

    @Test
    void descontarStockAplicaElMovimiento() throws Exception {
        String token = tokenDe(RECEPCIONISTA, CLAVE_DEMO);
        String codigo = primerCodigoDeInventario(token);

        var antes = mockMvc.perform(get("/api/farmacia/inventario")
                        .header("Authorization", "Bearer " + token))
                .andReturn();
        int stockAnterior = 0;
        var productos = cuerpo(antes).path("data");
        for (var producto : productos) {
            if (codigo.equals(producto.path("codigo").asText())) {
                stockAnterior = producto.path("stock").asInt();
            }
        }

        mockMvc.perform(post("/api/farmacia/inventario/" + codigo + "/descontar")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cantidad\":1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.stock").value(stockAnterior - 1));
    }

    @Test
    void descontarMasStockDelDisponibleDevuelve400() throws Exception {
        String token = tokenDe(RECEPCIONISTA, CLAVE_DEMO);
        String codigo = primerCodigoDeInventario(token);

        mockMvc.perform(post("/api/farmacia/inventario/" + codigo + "/descontar")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cantidad\":99999}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.ok").value(false));
    }

    @Test
    void movimientosDeInventarioSePuedenConsultar() throws Exception {
        String token = tokenDe(RECEPCIONISTA, CLAVE_DEMO);
        String codigo = primerCodigoDeInventario(token);

        mockMvc.perform(post("/api/farmacia/inventario/" + codigo + "/movimiento")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cantidad\":3}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ok").value(true));

        mockMvc.perform(get("/api/inventario/movimientos").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());
    }
}
