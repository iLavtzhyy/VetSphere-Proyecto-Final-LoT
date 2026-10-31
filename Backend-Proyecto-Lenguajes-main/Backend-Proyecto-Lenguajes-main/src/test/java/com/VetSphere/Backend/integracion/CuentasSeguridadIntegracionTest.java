package com.VetSphere.Backend.integracion;

import com.VetSphere.Backend.modules.clinic.domain.CodigoVerificacion;
import com.VetSphere.Backend.modules.clinic.repository.CodigoVerificacionRepository;
import com.VetSphere.Backend.modules.clinic.repository.UsuarioRepository;
import com.VetSphere.Backend.modules.clinic.domain.Factura;
import com.VetSphere.Backend.modules.clinic.domain.Mascota;
import com.VetSphere.Backend.modules.clinic.repository.FacturaRepository;
import com.VetSphere.Backend.modules.clinic.repository.MascotaRepository;
import com.VetSphere.Backend.modules.clinic.repository.ChatConversacionRepository;
import com.VetSphere.Backend.modules.clinic.repository.ChatMensajeRepository;
import com.VetSphere.Backend.modules.clinic.service.ChatMemoriaService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Control de la vida de la cuenta:
 * bloqueo automatico tras 3 intentos fallidos (HTTP 423), desbloqueo por el
 * administrador y baja/reactivacion del titular con verificacion por correo.
 *
 * Cada prueba usa un usuario propio para no dejar bloqueada ninguna cuenta
 * compartida por el resto de la suite.
 */
@Transactional
class CuentasSeguridadIntegracionTest extends BaseIntegracionTest {

    private static final String CLAVE = "Segura123";

    @Autowired
    private CodigoVerificacionRepository codigoRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private MascotaRepository mascotaRepository;

    @Autowired
    private FacturaRepository facturaRepository;

    @Autowired
    private ChatConversacionRepository chatConversacionRepository;

    @Autowired
    private ChatMensajeRepository chatMensajeRepository;

    @Autowired
    private ChatMemoriaService chatMemoriaService;

    @Autowired
    private EntityManager entityManager;

    /** Registra un usuario nuevo y lo deja verificado y activo. */
    private void crearUsuarioVerificado(String email) throws Exception {
        mockMvc.perform(post("/api/auth/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombres\":\"Usuario\",\"apellidos\":\"Prueba\",\"email\":\"" + email
                                + "\",\"documento\":\"1830000002\",\"password\":\"" + CLAVE + "\"}"))
                .andExpect(status().isOk());

        String codigo = ultimoCodigoDe(email, CodigoVerificacion.VERIFICACION_CUENTA);
        assertNotNull(codigo, "El registro debe emitir un codigo de verificacion");

        mockMvc.perform(post("/api/auth/verificar-2fa")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"codigo\":\"" + codigo + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.token").isNotEmpty());
    }

    private String ultimoCodigoDe(String email, String tipo) {
        return codigoRepository.findAll().stream()
                .filter(codigo -> email.equals(codigo.getEmail())
                        && tipo.equals(codigo.getTipo())
                        && !codigo.isUsado())
                .max(Comparator.comparing(CodigoVerificacion::getId))
                .map(CodigoVerificacion::getCodigo)
                .orElse(null);
    }

    private long idDeUsuario(String email) throws Exception {
        String token = tokenDe(ADMIN, CLAVE_DEMO);
        MvcResult resultado = mockMvc.perform(get("/api/admin/usuarios")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        long id = 0;
        for (var usuario : cuerpo(resultado).path("data")) {
            if (email.equals(usuario.path("email").asText())) {
                id = usuario.path("id").asLong();
            }
        }
        assertTrue(id > 0, "No se encontro el usuario " + email);
        return id;
    }

    private org.springframework.test.web.servlet.ResultActions intentoFallido(String email) throws Exception {
        return mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\",\"password\":\"clave-mala\"}"));
    }

    // ------------------------------------------------------------------
    // Bloqueo por intentos fallidos
    // ------------------------------------------------------------------

    @Test
    void tresIntentosFallidosBloqueanLaCuentaYLaSiguientePeticionDevuelve423() throws Exception {
        String email = "bloqueo.test@vetsphere.test";
        crearUsuarioVerificado(email);

        // Los dos primeros intentos fallidos solo devuelven 400.
        intentoFallido(email).andExpect(status().isBadRequest());
        intentoFallido(email).andExpect(status().isBadRequest());

        // El tercero deja la cuenta bloqueada: el mensaje sigue siendo neutro.
        intentoFallido(email)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("Credenciales incorrectas."));

        // La siguiente peticion ya recibe 423 (Locked) con el tiempo restante.
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + CLAVE + "\"}"))
                .andExpect(status().isLocked())
                .andExpect(jsonPath("$.ok").value(false))
                .andExpect(jsonPath("$.mensaje").isNotEmpty());
    }

    @Test
    void elAdministradorDesbloqueaLaCuenta() throws Exception {
        String email = "desbloqueo.test@vetsphere.test";
        crearUsuarioVerificado(email);
        long id = idDeUsuario(email);
        String tokenAdmin = tokenDe(ADMIN, CLAVE_DEMO);
        mockMvc.perform(put("/api/admin/usuarios/" + id + "/bloquear")
                        .header("Authorization", "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"horas\":3,\"motivo\":\"Prueba de bloqueo\"}"))
                .andExpect(status().isOk());

        // Con la cuenta bloqueada el login correcto tambien falla.
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + CLAVE + "\"}"))
                .andExpect(status().isLocked());

        mockMvc.perform(put("/api/admin/usuarios/" + id + "/desbloquear")
                        .header("Authorization", "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk());

        // Tras el desbloqueo el acceso vuelve a funcionar.
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + CLAVE + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.token").isNotEmpty());
    }

    @Test
    void elClienteNoPuedeBloquearUsuarios() throws Exception {
        String token = tokenDe(CLIENTE, CLAVE_DEMO);

        mockMvc.perform(put("/api/admin/usuarios/1/bloquear")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"horas\":3,\"motivo\":\"No permitido\"}"))
                .andExpect(status().isForbidden());
    }

    // ------------------------------------------------------------------
    // Baja y reactivacion por el titular
    // ------------------------------------------------------------------

    @Test
    void unUsuarioNoPuedeDesactivarUnaCuentaDesdeLaRutaPublica() throws Exception {
        String email = "baja.test@vetsphere.test";
        crearUsuarioVerificado(email);

        // Las rutas de baja quedan reservadas a administradores.
        mockMvc.perform(post("/api/auth/desactivar-cuenta")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\"}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/auth/confirmar-desactivacion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"codigo\":\"000000\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void unCodigoDeVerificacionNoSirveParaDesactivarLaCuenta() throws Exception {
        String email = "codigo.mal@vetsphere.test";
        crearUsuarioVerificado(email);

        mockMvc.perform(post("/api/auth/confirmar-desactivacion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"codigo\":\"111111\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void elTitularReactivaSuCuenta() throws Exception {
        String email = "reactivacion.test@vetsphere.test";
        crearUsuarioVerificado(email);
        long id = idDeUsuario(email);

        String tokenAdmin = tokenDe(ADMIN, CLAVE_DEMO);
        mockMvc.perform(put("/api/admin/usuarios/" + id + "/desactivar")
                        .header("Authorization", "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"motivo\":\"Prueba\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/auth/reactivar-cuenta")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"codigo\":\"000000\"}"))
                .andExpect(status().isOk());

        String codigo = ultimoCodigoDe(email, CodigoVerificacion.REACTIVACION);
        assertNotNull(codigo, "Debe emitirse un codigo de reactivacion");

        mockMvc.perform(post("/api/auth/confirmar-reactivacion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"codigo\":\"" + codigo + "\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + CLAVE + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.token").isNotEmpty());
    }

    @Test
    void elPerfilDevuelveElEstadoDeLaCuenta() throws Exception {
        String email = "perfil.test@vetsphere.test";
        crearUsuarioVerificado(email);

        String token = tokenDe(email, CLAVE);
        mockMvc.perform(get("/api/cuenta/perfil").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value(email))
                .andExpect(jsonPath("$.data.activo").value(true))
                .andExpect(jsonPath("$.data.roles").isArray());
    }

    @Test
    void unaCuentaDesactivadaNoPuedeUsarSuToken() throws Exception {
        String email = "token.invalido@vetsphere.test";
        crearUsuarioVerificado(email);
        long id = idDeUsuario(email);

        String token = tokenDe(email, CLAVE);
        String tokenAdmin = tokenDe(ADMIN, CLAVE_DEMO);
        mockMvc.perform(put("/api/admin/usuarios/" + id + "/desactivar")
                        .header("Authorization", "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"motivo\":\"Prueba\"}"))
                .andExpect(status().isOk());

        // El filtro JWT comprueba en cada peticion que la cuenta siga activa:
        // si no, el token deja de ser valido y la peticion responde 401.
        mockMvc.perform(get("/api/cuenta/perfil").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void elAdministradorEliminaPermanentementeUnaCuentaRegistrada() throws Exception {
        String email = "borrado.permanente@vetsphere.test";
        crearUsuarioVerificado(email);
        long id = idDeUsuario(email);
        Long conversacionId = chatMemoriaService.guardarTurnoLocal(email,
                "Consulta confidencial", "Respuesta guardada", null);
        String tokenAdmin = tokenDe(ADMIN, CLAVE_DEMO);
        var usuario = usuarioRepository.findById(id).orElseThrow();
        var mascota = mascotaRepository.save(Mascota.builder()
                .nombre("Paciente de cuenta eliminada")
                .estado("ACTIVA")
                .propietario(usuario)
                .build());
        facturaRepository.save(Factura.builder()
                .numero("TEST-DELETE-1")
                .fecha(java.time.LocalDateTime.now())
                .cliente(usuario)
                .mascota(mascota)
                .build());

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .delete("/api/admin/usuarios/" + id)
                        .header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mensaje").value(org.hamcrest.Matchers.containsString("permanentemente")));

        assertFalse(usuarioRepository.findByEmail(email).isPresent());
        assertFalse(chatConversacionRepository.findById(conversacionId).isPresent());
        assertEquals(0, chatMensajeRepository.countByConversacion_Id(conversacionId));
        entityManager.clear();
        assertEquals(null, mascotaRepository.findById(mascota.getId()).orElseThrow().getPropietario());
        assertEquals(null, facturaRepository.findAll().stream()
                .filter(factura -> "TEST-DELETE-1".equals(factura.getNumero()))
                .findFirst().orElseThrow().getCliente());
        mockMvc.perform(get("/api/admin/usuarios")
                        .header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.email == '" + email + "')]").isEmpty());
    }
}
