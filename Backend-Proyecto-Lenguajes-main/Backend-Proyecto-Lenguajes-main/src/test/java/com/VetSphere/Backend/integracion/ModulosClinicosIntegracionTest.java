package com.VetSphere.Backend.integracion;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Modulos que antes devolvian datos inventados (fachadas) y ahora persisten
 * en base de datos: vacunacion, seguimientos, urgencias, turnos, ordenes de
 * compra, imagenologia y consentimientos.
 *
 * <p>Se comprueba que los datos se guardan de verdad (y se vuelven a leer),
 * que las reglas de negocio se aplican y que solo la administracion puede
 * usarlos.</p>
 */
class ModulosClinicosIntegracionTest extends BaseIntegracionTest {

    @Autowired
    private com.VetSphere.Backend.modules.clinic.repository.MascotaRepository mascotaRepository;

    @Autowired
    private com.VetSphere.Backend.modules.clinic.repository.UsuarioRepository usuarioRepository;

    @Autowired
    private com.VetSphere.Backend.modules.clinic.repository.ProveedorRepository proveedorRepository;

    private Long idMascota() {
        return mascotaRepository.findAll().get(0).getId();
    }

    private Long idVeterinario() {
        return usuarioRepository.findByRolesNombre("ROLE_VETERINARIO").get(0).getId();
    }

    private String cabecera(String token) {
        return "Bearer " + token;
    }

    /** Devuelve un proveedor existente o crea uno de prueba. */
    private Long idProveedor() {
        return proveedorRepository.findAll().stream()
                .findFirst()
                .orElseGet(() -> proveedorRepository.save(
                        com.VetSphere.Backend.modules.clinic.domain.Proveedor.builder()
                                .nombre("Distribuidora Vet")
                                .contacto("Ventas")
                                .telefono("0999999999")
                                .email("ventas@distribuidoravet.com")
                                .direccion("Av. Principal 100")
                                .build()))
                .getId();
    }

    // ------------------------------------------------------------------
    // Vacunacion
    // ------------------------------------------------------------------

    @Test
    void laVacunacionSeGuardaYApareceEnElCalendario() throws Exception {
        String token = tokenDe(ADMIN, CLAVE_DEMO);

        MvcResult alta = mockMvc.perform(post("/api/vacunacion")
                        .header("Authorization", cabecera(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "mascotaId", idMascota(),
                                "vacuna", "Vacuna antirrabica",
                                "fecha", "2030-01-15",
                                "lote", "L-8891"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ok").value(true))
                .andExpect(jsonPath("$.data.vacuna").value("Vacuna antirrabica"))
                .andExpect(jsonPath("$.data.estado").value("Pendiente"))
                .andExpect(jsonPath("$.data.id").isNumber())
                .andReturn();

        long id = cuerpo(alta).path("data").path("id").asLong();
        assertTrue(id > 0);

        mockMvc.perform(get("/api/vacunacion/calendario").header("Authorization", cabecera(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.id == " + id + ")]").exists());

        mockMvc.perform(delete("/api/vacunacion/" + id).header("Authorization", cabecera(token)))
                .andExpect(status().isOk());
    }

    @Test
    void unaVacunacionSinMascotaSeRechaza() throws Exception {
        String token = tokenDe(ADMIN, CLAVE_DEMO);

        mockMvc.perform(post("/api/vacunacion")
                        .header("Authorization", cabecera(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("vacuna", "Rabia"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.ok").value(false))
                .andExpect(jsonPath("$.mensaje").value("Debes indicar la mascota de la vacunacion."));
    }

    @Test
    void unaVacunacionConEstadoInventadoSeRechaza() throws Exception {
        String token = tokenDe(ADMIN, CLAVE_DEMO);

        mockMvc.perform(post("/api/vacunacion")
                        .header("Authorization", cabecera(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "mascotaId", idMascota(),
                                "vacuna", "Rabia",
                                "estado", "Inventado"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.ok").value(false));
    }

    // ------------------------------------------------------------------
    // Seguimientos
    // ------------------------------------------------------------------

    @Test
    void elSeguimientoPostoperatorioQuedaRegistrado() throws Exception {
        String token = tokenDe(ADMIN, CLAVE_DEMO);

        MvcResult alta = mockMvc.perform(post("/api/seguimientos")
                        .header("Authorization", cabecera(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "mascotaId", idMascota(),
                                "indicacion", "Control de suturas a los 10 dias",
                                "tipo", "Postoperatorio",
                                "fecha", "2030-02-01",
                                "veterinarioId", idVeterinario()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.tipo").value("Postoperatorio"))
                .andExpect(jsonPath("$.data.estado").value("Pendiente"))
                .andReturn();

        long id = cuerpo(alta).path("data").path("id").asLong();

        mockMvc.perform(get("/api/seguimientos/postoperatorios").header("Authorization", cabecera(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.id == " + id + ")]").exists());

        mockMvc.perform(delete("/api/seguimientos/" + id).header("Authorization", cabecera(token)))
                .andExpect(status().isOk());
    }

    @Test
    void unSeguimientoSinIndicacionSeRechaza() throws Exception {
        String token = tokenDe(ADMIN, CLAVE_DEMO);

        mockMvc.perform(post("/api/seguimientos")
                        .header("Authorization", cabecera(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("mascotaId", idMascota()))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("Debes indicar la indicacion del seguimiento."));
    }

    // ------------------------------------------------------------------
    // Urgencias
    // ------------------------------------------------------------------

    @Test
    void laUrgenciaRegistradaApareceEntreLasActivas() throws Exception {
        String token = tokenDe(ADMIN, CLAVE_DEMO);

        MvcResult alta = mockMvc.perform(post("/api/urgencias")
                        .header("Authorization", cabecera(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "mascotaId", idMascota(),
                                "motivo", "Dificultad respiratoria",
                                "prioridad", "Alta",
                                "horaIngreso", "2030-03-01T10:15:00"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.prioridad").value("Alta"))
                .andExpect(jsonPath("$.data.estado").value("En curso"))
                .andReturn();

        long id = cuerpo(alta).path("data").path("id").asLong();

        mockMvc.perform(get("/api/urgencias/activas").header("Authorization", cabecera(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.id == " + id + ")]").exists());

        mockMvc.perform(delete("/api/urgencias/" + id).header("Authorization", cabecera(token)))
                .andExpect(status().isOk());
    }

    @Test
    void unaUrgenciaConPrioridadInventadaSeRechaza() throws Exception {
        String token = tokenDe(ADMIN, CLAVE_DEMO);

        mockMvc.perform(post("/api/urgencias")
                        .header("Authorization", cabecera(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "mascotaId", idMascota(),
                                "motivo", "Cojera",
                                "prioridad", "Urgente"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.ok").value(false));
    }

    // ------------------------------------------------------------------
    // Turnos del personal
    // ------------------------------------------------------------------

    @Test
    void elTurnoSeRegistraYNoAdmiteSolapamientos() throws Exception {
        String token = tokenDe(ADMIN, CLAVE_DEMO);
        Long empleado = idVeterinario();

        MvcResult alta = mockMvc.perform(post("/api/turnos")
                        .header("Authorization", cabecera(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "empleadoId", empleado,
                                "fecha", "2030-04-01",
                                "horaInicio", "08:00:00",
                                "horaFin", "14:00:00",
                                "area", "Recepcion"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.area").value("Recepcion"))
                .andReturn();

        long id = cuerpo(alta).path("data").path("id").asLong();

        mockMvc.perform(get("/api/turnos").header("Authorization", cabecera(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.id == " + id + ")]").exists());

        // Un turno que se cruza con el anterior no se admite.
        mockMvc.perform(post("/api/turnos")
                        .header("Authorization", cabecera(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "empleadoId", empleado,
                                "fecha", "2030-04-01",
                                "horaInicio", "13:00:00",
                                "horaFin", "18:00:00",
                                "area", "Clinica"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value(org.hamcrest.Matchers.containsString("ya tiene un turno")));

        mockMvc.perform(delete("/api/turnos/" + id).header("Authorization", cabecera(token)))
                .andExpect(status().isOk());
    }

    @Test
    void unTurnoConHoraFinalAnteriorSeRechaza() throws Exception {
        String token = tokenDe(ADMIN, CLAVE_DEMO);

        mockMvc.perform(post("/api/turnos")
                        .header("Authorization", cabecera(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "empleadoId", idVeterinario(),
                                "fecha", "2030-05-01",
                                "horaInicio", "14:00:00",
                                "horaFin", "08:00:00",
                                "area", "Clinica"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje")
                        .value("La hora de fin del turno debe ser posterior a la hora de inicio."));
    }

    // ------------------------------------------------------------------
    // Ordenes de compra
    // ------------------------------------------------------------------

    @Test
    void laOrdenDeCompraRecibeElNumeroCorrelativoDelServidor() throws Exception {
        String token = tokenDe(ADMIN, CLAVE_DEMO);
        Long proveedor = idProveedor();

        MvcResult alta = mockMvc.perform(post("/api/compras/ordenes")
                        .header("Authorization", cabecera(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "proveedorId", proveedor,
                                "fecha", "2030-06-01",
                                "total", 320.50,
                                "detalle", "Lote de_DMA y sutures"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.estado").value("Solicitada"))
                .andReturn();

        JsonNode data = cuerpo(alta).path("data");
        String numero = data.path("numero").asText();
        assertTrue(numero.matches("OC-\\d{4}-\\d{4}"), "Numero inesperado: " + numero);

        mockMvc.perform(get("/api/compras/ordenes").header("Authorization", cabecera(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.numero == '" + numero + "')]").exists());

        mockMvc.perform(delete("/api/compras/ordenes/" + data.path("id").asLong())
                        .header("Authorization", cabecera(token)))
                .andExpect(status().isOk());
    }

    @Test
    void unaOrdenDeCompraConNumeroRepetidoSeRechaza() throws Exception {
        String token = tokenDe(ADMIN, CLAVE_DEMO);
        Long proveedor = idProveedor();
        String numero = "OC-DUPLICADO-1";

        mockMvc.perform(post("/api/compras/ordenes")
                        .header("Authorization", cabecera(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "proveedorId", proveedor,
                                "fecha", "2030-06-02",
                                "numero", numero,
                                "total", 10))))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/compras/ordenes")
                        .header("Authorization", cabecera(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "proveedorId", proveedor,
                                "fecha", "2030-06-03",
                                "numero", numero,
                                "total", 10))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("Ya existe una orden de compra con el numero " + numero + "."));
    }

    @Test
    void unaOrdenDeCompraConTotalNegativoSeRechaza() throws Exception {
        String token = tokenDe(ADMIN, CLAVE_DEMO);
        Long proveedor = idProveedor();

        mockMvc.perform(post("/api/compras/ordenes")
                        .header("Authorization", cabecera(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "proveedorId", proveedor,
                                "fecha", "2030-06-04",
                                "total", -5))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.ok").value(false));
    }

    // ------------------------------------------------------------------
    // Imagenologia
    // ------------------------------------------------------------------

    @Test
    void elEstudioDeImagenologiaSeGuardaYSeConsulta() throws Exception {
        String token = tokenDe(ADMIN, CLAVE_DEMO);

        MvcResult alta = mockMvc.perform(post("/api/imagenologia")
                        .header("Authorization", cabecera(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "mascotaId", idMascota(),
                                "tipo", "Radiografia",
                                "fecha", "2030-07-01",
                                "informe", "Sin alteraciones osteoarticulares relevantes.",
                                "veterinarioId", idVeterinario()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.tipo").value("Radiografia"))
                .andExpect(jsonPath("$.data.estado").value("Solicitado"))
                .andReturn();

        long id = cuerpo(alta).path("data").path("id").asLong();

        mockMvc.perform(get("/api/imagenologia/estudios").header("Authorization", cabecera(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.id == " + id + ")]").exists());

        mockMvc.perform(get("/api/imagenologia/mascota/" + idMascota())
                        .header("Authorization", cabecera(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.id == " + id + ")]").exists());

        mockMvc.perform(delete("/api/imagenologia/" + id).header("Authorization", cabecera(token)))
                .andExpect(status().isOk());
    }

    @Test
    void unEstudioConTipoNoSoportadoSeRechaza() throws Exception {
        String token = tokenDe(ADMIN, CLAVE_DEMO);

        mockMvc.perform(post("/api/imagenologia")
                        .header("Authorization", cabecera(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "mascotaId", idMascota(),
                                "tipo", "Ecografia Doppler"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje")
                        .value("El tipo de estudio debe ser Radiografia, Ecografia, TAC o Resonancia."));
    }

    // ------------------------------------------------------------------
    // Consentimientos
    // ------------------------------------------------------------------

    @Test
    void elConsentimientoQuirurgicoSePersisteConSuFolio() throws Exception {
        String token = tokenDe(ADMIN, CLAVE_DEMO);

        MvcResult alta = mockMvc.perform(post("/api/consentimientos/quirurgico")
                        .header("Authorization", cabecera(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "mascotaId", idMascota(),
                                "firmadoPor", "Ana Molina",
                                "cargo", "Propietaria",
                                "documento", "Autorizo la intervencion quirurgica."))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.tipo").value("Quirurgico"))
                .andExpect(jsonPath("$.data.estado").value("Firmado"))
                .andReturn();

        JsonNode data = cuerpo(alta).path("data");
        String folio = data.path("folio").asText();
        assertTrue(folio.matches("CONS-\\d{4}-\\d{4}"), "Folio inesperado: " + folio);

        mockMvc.perform(get("/api/consentimientos").header("Authorization", cabecera(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.folio == '" + folio + "')]").exists());

        mockMvc.perform(delete("/api/consentimientos/" + data.path("id").asLong())
                        .header("Authorization", cabecera(token)))
                .andExpect(status().isOk());
    }

    @Test
    void unConsentimientoSinMascotaSeRechaza() throws Exception {
        String token = tokenDe(ADMIN, CLAVE_DEMO);

        mockMvc.perform(post("/api/consentimientos/quirurgico")
                        .header("Authorization", cabecera(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("firmadoPor", "Ana Molina"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("Debes indicar la mascota del consentimiento."));
    }

    // ------------------------------------------------------------------
    // Permisos de los modulos nuevos
    // ------------------------------------------------------------------

    @Test
    void losModulosNuevosSoloLosAdministraElAdministrador() throws Exception {
        String token = tokenDe(RECEPCIONISTA, CLAVE_DEMO);

        mockMvc.perform(get("/api/vacunacion/calendario").header("Authorization", cabecera(token)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/seguimientos/postoperatorios").header("Authorization", cabecera(token)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/urgencias/activas").header("Authorization", cabecera(token)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/turnos").header("Authorization", cabecera(token)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/compras/ordenes").header("Authorization", cabecera(token)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/imagenologia/estudios").header("Authorization", cabecera(token)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/consentimientos").header("Authorization", cabecera(token)))
                .andExpect(status().isForbidden());
    }

    @Test
    void sinTokenLosModulosNuevosDevuelven401() throws Exception {
        mockMvc.perform(get("/api/turnos"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.ok").value(false));
    }

    @Test
    void elVeterinarioTampocoAdministraLosModulosReservados() throws Exception {
        String token = tokenDe("sofia.cardenas@vetsphere.com", CLAVE_DEMO);

        mockMvc.perform(get("/api/vacunacion").header("Authorization", cabecera(token)))
                .andExpect(status().isForbidden());
    }

    @Test
    void laActualizacionDeUnTurnoNoSeComparaConsigoMismo() throws Exception {
        String token = tokenDe(ADMIN, CLAVE_DEMO);
        Long empleado = idVeterinario();

        MvcResult alta = mockMvc.perform(post("/api/turnos")
                        .header("Authorization", cabecera(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "empleadoId", empleado,
                                "fecha", "2030-08-01",
                                "horaInicio", "08:00:00",
                                "horaFin", "14:00:00",
                                "area", "Recepcion"))))
                .andExpect(status().isOk())
                .andReturn();

        long id = cuerpo(alta).path("data").path("id").asLong();

        mockMvc.perform(put("/api/turnos/" + id)
                        .header("Authorization", cabecera(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "empleadoId", empleado,
                                "fecha", "2030-08-01",
                                "horaInicio", "09:00:00",
                                "horaFin", "15:00:00",
                                "area", "Recepcion"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.horaInicio").value("09:00:00"));

        mockMvc.perform(delete("/api/turnos/" + id).header("Authorization", cabecera(token)))
                .andExpect(status().isOk());
    }

    @Test
    void laActualizacionMantieneElFolioDelConsentimiento() throws Exception {
        String token = tokenDe(ADMIN, CLAVE_DEMO);

        MvcResult alta = mockMvc.perform(post("/api/consentimientos")
                        .header("Authorization", cabecera(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "mascotaId", idMascota(),
                                "tipo", "Tratamiento",
                                "firmadoPor", "Carlos Ruiz"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.estado").value("Pendiente"))
                .andReturn();

        JsonNode data = cuerpo(alta).path("data");
        long id = data.path("id").asLong();
        String folio = data.path("folio").asText();
        assertNotNull(folio);

        mockMvc.perform(put("/api/consentimientos/" + id)
                        .header("Authorization", cabecera(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "mascotaId", idMascota(),
                                "estado", "Firmado",
                                "observaciones", "Firmado en recepcion."))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.estado").value("Firmado"))
                .andExpect(jsonPath("$.data.folio").value(folio));

        mockMvc.perform(delete("/api/consentimientos/" + id).header("Authorization", cabecera(token)))
                .andExpect(status().isOk());
    }
}
