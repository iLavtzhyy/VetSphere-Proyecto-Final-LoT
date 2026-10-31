package com.VetSphere.Backend.modules.clinic.service;

import com.VetSphere.Backend.modules.clinic.domain.Mascota;
import com.VetSphere.Backend.modules.clinic.domain.Producto;
import com.VetSphere.Backend.modules.clinic.dto.ChatResponse;
import com.VetSphere.Backend.modules.clinic.repository.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.junit.jupiter.api.AfterEach;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias del asistente inteligente: sin API key responde un
 * mensaje amigable, con API key invoca el modelo (y las herramientas), y
 * cualquier fallo de red se traduce en un mensaje controlado.
 */
class ChatServiceTest {

    private static final ObjectMapper MAPEADOR = new ObjectMapper();

    private RestClient restClient;
    private RestClient.RequestBodyUriSpec uriSpec;
    private RestClient.RequestBodySpec bodySpec;
    private RestClient.ResponseSpec responseSpec;
    private MascotaRepository mascotaRepository;
    private ProductoRepository productoRepository;

    @BeforeEach
    void configurar() {
        restClient = mock(RestClient.class);
        uriSpec = mock(RestClient.RequestBodyUriSpec.class);
        bodySpec = mock(RestClient.RequestBodySpec.class);
        responseSpec = mock(RestClient.ResponseSpec.class);
        mascotaRepository = mock(MascotaRepository.class);
        productoRepository = mock(ProductoRepository.class);

        when(restClient.post()).thenReturn(uriSpec);
        when(uriSpec.uri(anyString(), any(Object[].class))).thenReturn(bodySpec);
        when(bodySpec.contentType(any(MediaType.class))).thenReturn(bodySpec);
        when(bodySpec.header(anyString(), any(String[].class))).thenReturn(bodySpec);
        // any(Object.class) fija la sobrecarga body(Object); con any() puro Java
        // resuelve body(StreamingHttpOutputMessage.Body) y el stub no aplica.
        when(bodySpec.body(any(Object.class))).thenReturn(bodySpec);
        when(bodySpec.retrieve()).thenReturn(responseSpec);
    }

    private ChatService servicioConApiKey(String apiKey) {
        return servicioConApiKey(apiKey, productoRepository);
    }

    private ChatService servicioConApiKey(String apiKey, ProductoRepository productos) {
        RestClient.Builder builder = mock(RestClient.Builder.class);
        when(builder.baseUrl(anyString())).thenReturn(builder);
        when(builder.requestFactory(any())).thenReturn(builder);
        when(builder.build()).thenReturn(restClient);

        return new ChatService(builder, MAPEADOR, mascotaRepository, mock(UsuarioRepository.class),
                productos, mock(CitaRepository.class), mock(FacturaRepository.class),
                mock(MascotaService.class), mock(CuponService.class), "https://api.openai.com/v1", apiKey, "gpt-4o-mini");
    }

    @AfterEach
    void limpiarAutenticacion() {
        SecurityContextHolder.clearContext();
    }

    private JsonNode respuesta(String json) throws Exception {
        return MAPEADOR.readTree(json);
    }

    @Test
    void sinClaveApiMantieneRespuestasLocalesSinInvocarAlModelo() {
        ChatService servicio = servicioConApiKey("");

        ChatResponse respuesta = servicio.preguntar("Hola, como esta mi mascota?");

        assertTrue(respuesta.asistenteConfigurado());
        assertTrue(respuesta.consultoBaseDatos());
        assertTrue(respuesta.respuesta().contains("No encontré mascotas"));
        verifyNoInteractions(restClient);
    }

    @Test
    void sinClaveApiRespondeSaludosLocalmente() {
        ChatResponse respuesta = servicioConApiKey("").preguntar("hola");
        assertTrue(respuesta.asistenteConfigurado());
        assertTrue(respuesta.respuesta().contains("¡Hola!"));
        verifyNoInteractions(restClient);
    }

    @Test
    void entiendeConsultaConversacionalDeProductosBajoStock() {
        SecurityContextHolder.getContext().setAuthentication(
                new TestingAuthenticationToken("admin", "", "ROLE_ADMIN"));
        Producto critico = Producto.builder().id(1L).codigo("MED-1").nombre("Amoxicilina")
                .stock(2).stockCritico(5).build();
        Producto normal = Producto.builder().id(2L).codigo("MED-2").nombre("Vitaminas")
                .stock(20).stockCritico(5).build();
        when(productoRepository.findAll()).thenReturn(List.of(critico, normal));

        ChatResponse respuesta = servicioConApiKey("sk-test").preguntar("dime cuales son los q estan bajo stock");

        assertTrue(respuesta.consultoBaseDatos());
        assertTrue(respuesta.respuesta().contains("1 productos bajo stock"));
        assertTrue(respuesta.respuesta().contains("Amoxicilina (2 disponibles; mínimo 5)"));
        verifyNoInteractions(restClient);
    }

    @Test
    void respuestaDirectaDelModeloSeDevuelveTalCual() throws Exception {
        when(responseSpec.body(JsonNode.class)).thenReturn(respuesta(
                "{\"choices\":[{\"message\":{\"content\":\"Todo en orden, revisa su vacunacion.\"}}]}"));

        ChatResponse respuesta = servicioConApiKey("sk-test").preguntar("Como va Luna?");

        assertTrue(respuesta.asistenteConfigurado());
        assertFalse(respuesta.consultoBaseDatos());
        assertEquals("Todo en orden, revisa su vacunacion.", respuesta.respuesta());
    }

    @Test
    void informaCuandoElProveedorApiAgotaElSaldoDisponible() {
        when(responseSpec.body(JsonNode.class)).thenThrow(new HttpClientErrorException(HttpStatus.PAYMENT_REQUIRED));

        ChatResponse respuesta = servicioConApiKey("cc-test-key").preguntar("Explícame qué hace una vacuna");

        assertTrue(respuesta.asistenteConfigurado());
        assertTrue(respuesta.respuesta().contains("problema de facturación o saldo"));
    }

    @Test
    void cuandoElModeloPideHerramientasSeEjecutanYSeLeDevuelvenLosResultados() throws Exception {
        JsonNode peticionHerramienta = respuesta("{\"choices\":[{\"message\":{\"content\":null,\"tool_calls\":["
                + "{\"id\":\"call_1\",\"type\":\"function\",\"function\":{\"name\":\"buscar_mascota\","
                + "\"arguments\":\"{\\\"texto\\\":\\\"Luna\\\"}\"}}]}}]}");
        JsonNode respuestaFinal = respuesta("{\"choices\":[{\"message\":{\"content\":\"Luna es una gata de 3 anios.\"}}]}");
        when(responseSpec.body(JsonNode.class)).thenReturn(peticionHerramienta, respuestaFinal);

        Mascota luna = Mascota.builder().id(1L).nombre("Luna").especie("Gato").raza("Comun").estado("ACTIVA").build();
        when(mascotaRepository.findAll()).thenReturn(List.of(luna));

        ChatResponse respuesta = servicioConApiKey("sk-test").preguntar("Tengo una mascota llamada Luna?");

        assertTrue(respuesta.consultoBaseDatos());
        assertEquals("Luna es una gata de 3 anios.", respuesta.respuesta());
        // Dos llamadas al modelo: una pidiendo la herramienta y otra con el resultado
        verify(restClient, times(2)).post();
    }

    @Test
    void errorDeRedDevuelveMensajeControladoSinExplotar() {
        when(responseSpec.body(JsonNode.class)).thenThrow(new IllegalStateException("conexion rechazada"));

        ChatResponse respuesta = servicioConApiKey("sk-test").preguntar("Hola");

        assertTrue(respuesta.asistenteConfigurado());
        assertTrue(respuesta.respuesta().contains("¡Hola!"));
    }

    @Test
    void respuestaVaciaDelModeloSeTraduceEnMensajeAmigable() throws Exception {
        when(responseSpec.body(JsonNode.class)).thenReturn(respuesta(
                "{\"choices\":[{\"message\":{\"content\":\"\"}}]}"));

        ChatResponse respuesta = servicioConApiKey("sk-test").preguntar("Algo raro");

        assertTrue(respuesta.respuesta().contains("reformular"));
    }
}
