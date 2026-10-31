package com.VetSphere.Backend.integracion;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Farmacia comercial: analisis de rotacion (que productos no se venden),
 * gestion de cupones de descuento y aplicacion del cupon en la factura.
 *
 * Comprueba que el descuento lo calcula el servidor y que la gestion de
 * cupones queda reservada al administrador.
 */
@Transactional
class PromocionesFarmaciaIntegracionTest extends BaseIntegracionTest {

    private String crearCuponGlobal(String codigo, double porcentaje) throws Exception {
        String token = tokenDe(ADMIN, CLAVE_DEMO);
        String cuerpo = "{"
                + "\"codigo\":\"" + codigo + "\","
                + "\"descripcion\":\"Promocion de prueba\","
                + "\"tipoDescuento\":\"PORCENTAJE\","
                + "\"valor\":" + porcentaje + ","
                + "\"fechaInicio\":\"2020-01-01T00:00:00\","
                + "\"fechaFin\":\"2099-12-31T00:00:00\","
                + "\"activo\":true}";

        MvcResult resultado = mockMvc.perform(post("/api/farmacia/cupones")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ok").value(true))
                .andReturn();
        return cuerpo(resultado).path("data").path("codigo").asText();
    }

    // ------------------------------------------------------------------
    // Rotacion de inventario
    // ------------------------------------------------------------------

    @Test
    void elAnalisisDeRotacionDevuelveResumenYDetallePorProducto() throws Exception {
        String token = tokenDe(ADMIN, CLAVE_DEMO);

        mockMvc.perform(get("/api/farmacia/rotacion").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ok").value(true))
                .andExpect(jsonPath("$.data.resumen.totalProductos").isNumber())
                .andExpect(jsonPath("$.data.resumen.diasVentana").isNumber())
                .andExpect(jsonPath("$.data.resumen.diasUmbral").isNumber())
                .andExpect(jsonPath("$.data.resumen.capitalDormido").isNumber())
                .andExpect(jsonPath("$.data.productos").isArray())
                .andExpect(jsonPath("$.data.productos[0].codigo").isNotEmpty())
                .andExpect(jsonPath("$.data.productos[0].estado").isNotEmpty());
    }

    @Test
    void elAnalisisRespetaLaVentanaYElUmbralSolicitados() throws Exception {
        String token = tokenDe(ADMIN, CLAVE_DEMO);

        mockMvc.perform(get("/api/farmacia/rotacion")
                        .param("dias", "60")
                        .param("umbral", "45")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.resumen.diasVentana").value(60))
                .andExpect(jsonPath("$.data.resumen.diasUmbral").value(45));
    }

    @Test
    void elRecepcionistaPuedeConsultarElAnalisisDeRotacion() throws Exception {
        String token = tokenDe(RECEPCIONISTA, CLAVE_DEMO);

        mockMvc.perform(get("/api/farmacia/rotacion").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.productos").isArray());
    }

    @Test
    void elClienteNoPuedeConsultarElAnalisisDeRotacion() throws Exception {
        String token = tokenDe(CLIENTE, CLAVE_DEMO);

        mockMvc.perform(get("/api/farmacia/rotacion").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    // ------------------------------------------------------------------
    // Cupones
    // ------------------------------------------------------------------

    @Test
    void elAdminCreaUnCuponYElRecepcionistaNoPuede() throws Exception {
        String codigo = crearCuponGlobal("PROMO-TEST-10", 10);
        assertEquals("PROMO-TEST-10", codigo);

        String tokenRecepcionista = tokenDe(RECEPCIONISTA, CLAVE_DEMO);
        mockMvc.perform(post("/api/farmacia/cupones")
                        .header("Authorization", "Bearer " + tokenRecepcionista)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tipoDescuento\":\"PORCENTAJE\",\"valor\":5,\"activo\":true}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void laSimulacionDevuelveElDescuentoCalculadoPorElServidor() throws Exception {
        crearCuponGlobal("PROMO-SIM-15", 15);
        String token = tokenDe(ADMIN, CLAVE_DEMO);

        MvcResult resultado = mockMvc.perform(post("/api/farmacia/cupones/validar")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"codigo\":\"PROMO-SIM-15\",\"concepto\":\"Venta de prueba\",\"subtotal\":100.00}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.aplicado").value(true))
                .andExpect(jsonPath("$.data.codigo").value("PROMO-SIM-15"))
                .andReturn();

        var datos = cuerpo(resultado).path("data");
        assertEquals(15.0, datos.path("descuento").asDouble(), 0.001);
        assertEquals(85.0, datos.path("subtotal").asDouble(), 0.001);
        assertEquals(12.75, datos.path("impuestos").asDouble(), 0.001);
        assertEquals(97.75, datos.path("total").asDouble(), 0.001);
    }

    @Test
    void unCuponInexistenteDevuelve400ConMensajeClaro() throws Exception {
        String token = tokenDe(ADMIN, CLAVE_DEMO);

        mockMvc.perform(post("/api/farmacia/cupones/validar")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"codigo\":\"NO-EXISTE\",\"subtotal\":100.00}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.ok").value(false))
                .andExpect(jsonPath("$.mensaje").isNotEmpty());
    }

    @Test
    void unCuponDesactivadoDejaDeAplicarse() throws Exception {
        crearCuponGlobal("PROMO-OFF-20", 20);
        String token = tokenDe(ADMIN, CLAVE_DEMO);

        var listado = mockMvc.perform(get("/api/farmacia/cupones")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        long id = 0;
        for (var cupon : cuerpo(listado).path("data")) {
            if ("PROMO-OFF-20".equals(cupon.path("codigo").asText())) {
                id = cupon.path("id").asLong();
            }
        }
        assertTrue(id > 0, "El cupon deberia existir en el listado");

        mockMvc.perform(put("/api/farmacia/cupones/" + id + "/estado")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"activo\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.activo").value(false));

        mockMvc.perform(post("/api/farmacia/cupones/validar")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"codigo\":\"PROMO-OFF-20\",\"subtotal\":100.00}"))
                .andExpect(status().isBadRequest());
    }

    // ------------------------------------------------------------------
    // Factura con cupon
    // ------------------------------------------------------------------

    @Test
    void laFacturaRegistraElDescuentoYElCodigoDelCupon() throws Exception {
        crearCuponGlobal("PROMO-FACT-10", 10);
        String token = tokenDe(ADMIN, CLAVE_DEMO);

        MvcResult resultado = mockMvc.perform(post("/api/facturacion")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"concepto\":\"Venta con promocion\",\"subtotal\":200.00,"
                                + "\"cuponCodigo\":\"PROMO-FACT-10\",\"estadoPago\":\"Pagado\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.cuponCodigo").value("PROMO-FACT-10"))
                .andExpect(jsonPath("$.data.descuento").value(20.0))
                .andExpect(jsonPath("$.data.subtotal").value(180.0))
                .andExpect(jsonPath("$.data.impuestos").value(27.0))
                .andExpect(jsonPath("$.data.total").value(207.0))
                .andExpect(jsonPath("$.data.numero").isNotEmpty())
                .andReturn();

        String numero = cuerpo(resultado).path("data").path("numero").asText();
        assertTrue(numero.startsWith("FAC-"), "La numeracion la genera el servidor: " + numero);
    }

    @Test
    void unaFacturaSinCuponNoLlevaDescuento() throws Exception {
        String token = tokenDe(ADMIN, CLAVE_DEMO);

        mockMvc.perform(post("/api/facturacion")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"concepto\":\"Venta sin promocion\",\"subtotal\":50.00}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.descuento").value(0))
                .andExpect(jsonPath("$.data.total").value(57.5));
    }

    @Test
    void lasPromocionesAutomaticasSeGeneranParaLaBajaRotacion() throws Exception {
        String token = tokenDe(ADMIN, CLAVE_DEMO);

        mockMvc.perform(post("/api/farmacia/cupones/auto")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"diasSinVenta\":30,\"descuentoPorcentaje\":15,\"duracionDias\":15,"
                                + "\"descripcion\":\"Promocion automatica de baja rotacion\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ok").value(true))
                .andExpect(jsonPath("$.data.creados").isNumber())
                .andExpect(jsonPath("$.data.cupones").isArray());
    }
}

