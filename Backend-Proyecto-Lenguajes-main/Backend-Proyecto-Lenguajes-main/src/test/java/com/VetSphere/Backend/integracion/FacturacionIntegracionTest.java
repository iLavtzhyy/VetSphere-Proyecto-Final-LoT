package com.VetSphere.Backend.integracion;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * El contrato de facturacion con el frontend: el subtotal del cliente se
 * sanea y los impuestos (IVA 15%) y el total SIEMPRE se recalculan aqui.
 */
@Transactional
class FacturacionIntegracionTest extends BaseIntegracionTest {

    @Test
    void crearFacturaRecalculaImpuestosYTotalEnElServidor() throws Exception {
        String token = tokenDe(RECEPCIONISTA, CLAVE_DEMO);

        // El payload imita al frontend de farmacia: manda sus totales, pero se ignoran
        var resultado = mockMvc.perform(post("/api/facturacion")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"concepto\":\"Venta farmacia\",\"subtotal\":100.00,"
                                + "\"impuestos\":1.00,\"total\":2.00}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ok").value(true))
                .andExpect(jsonPath("$.data.estadoPago").value("Pendiente"))
                .andReturn();

        var factura = cuerpo(resultado).path("data");
        assertEquals(0, new BigDecimal("100.00").compareTo(factura.path("subtotal").decimalValue()));
        assertEquals(0, new BigDecimal("15.00").compareTo(factura.path("impuestos").decimalValue()));
        assertEquals(0, new BigDecimal("115.00").compareTo(factura.path("total").decimalValue()));
    }

    @Test
    void facturaSinConceptoDevuelve400() throws Exception {
        String token = tokenDe(RECEPCIONISTA, CLAVE_DEMO);

        mockMvc.perform(post("/api/facturacion")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"subtotal\":10.00}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.ok").value(false));
    }

    @Test
    void facturaSinSubtotalValidoDevuelve400() throws Exception {
        String token = tokenDe(RECEPCIONISTA, CLAVE_DEMO);

        mockMvc.perform(post("/api/facturacion")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"concepto\":\"Sin precio\",\"subtotal\":0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.ok").value(false));
    }

    @Test
    void listadoDeFacturasConservaElArregloPlanoYSoportaPaginacion() throws Exception {
        String token = tokenDe(RECEPCIONISTA, CLAVE_DEMO);

        mockMvc.perform(get("/api/facturacion").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());

        mockMvc.perform(get("/api/facturacion?page=0&size=2").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.contenido").isArray())
                .andExpect(jsonPath("$.data.totalElementos").isNumber());
    }

    @Test
    void actualizarEstadoDePagoDeUnaFactura() throws Exception {
        String token = tokenDe(RECEPCIONISTA, CLAVE_DEMO);
        var creacion = mockMvc.perform(post("/api/facturacion")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"concepto\":\"Consulta\",\"subtotal\":45.00}"))
                .andExpect(status().isOk())
                .andReturn();
        long id = cuerpo(creacion).path("data").path("id").asLong();

        mockMvc.perform(put("/api/facturacion/" + id + "/estado")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"estadoPago\":\"Pagado\",\"metodoPago\":\"Transferencia Banco Pichincha\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.estadoPago").value("Pagado"))
                .andExpect(jsonPath("$.data.metodoPago").value("Transferencia Banco Pichincha"));
    }

    @Test
    void facturaSeDescargaEnPdf() throws Exception {
        String token = tokenDe(RECEPCIONISTA, CLAVE_DEMO);
        var creacion = mockMvc.perform(post("/api/facturacion")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"concepto\":\"Consulta vacunacion\",\"subtotal\":50.00}"))
                .andExpect(status().isOk())
                .andReturn();
        long id = cuerpo(creacion).path("data").path("id").asLong();

        var resultado = mockMvc.perform(get("/api/facturas/" + id + "/pdf")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/pdf"))
                .andExpect(header().string("Content-Disposition", containsString("attachment")))
                .andReturn();

        // El cuerpo viaja como bytes; se decodifica como ISO-8859-1 para leer el encabezado %PDF
        String pdf = new String(resultado.getResponse().getContentAsByteArray(), StandardCharsets.ISO_8859_1);
        assertTrue(pdf.startsWith("%PDF"), "El PDF debe empezar por %PDF pero empieza por: "
                + pdf.substring(0, Math.min(12, pdf.length())));
    }

    @Test
    void pdfDeUnaFacturaInexistenteDevuelve404ConEnvoltorioApi() throws Exception {
        String token = tokenDe(RECEPCIONISTA, CLAVE_DEMO);

        mockMvc.perform(get("/api/facturas/999999/pdf")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.ok").value(false));
    }

    @Test
    void elPdfDeFacturasNoEstaDisponibleParaClientes() throws Exception {
        String token = tokenDe(CLIENTE, CLAVE_DEMO);

        mockMvc.perform(get("/api/facturas/1/pdf")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.ok").value(false));
    }
}

