package com.VetSphere.Backend.integracion;

import com.VetSphere.Backend.modules.clinic.domain.CodigoVerificacion;
import com.VetSphere.Backend.modules.clinic.repository.CodigoVerificacionRepository;
import com.VetSphere.Backend.modules.clinic.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Regresión para endpoints que invalidan códigos sin transacción externa de prueba. */
class AuthTransaccionesIntegracionTest extends BaseIntegracionTest {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private CodigoVerificacionRepository codigoRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void recuperacionYLoginConSegundoFactorNoDevuelven500PorFaltaDeTransaccion() throws Exception {
        mockMvc.perform(post("/api/auth/solicitar-recuperacion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + ADMIN + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ok").value(true));

        var admin = usuarioRepository.findByEmail(ADMIN).orElseThrow();
        Boolean segundoFactorOriginal = admin.getSegundoFactorCorreoHabilitado();
        Boolean autenticadorOriginal = admin.getAutenticadorHabilitado();
        String respaldosOriginales = admin.getCodigosRespaldoHash();
        try {
            admin.setSegundoFactorCorreoHabilitado(true);
            admin.setAutenticadorHabilitado(false);
            admin.setCodigosRespaldoHash(null);
            usuarioRepository.save(admin);

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"email\":\"" + ADMIN + "\",\"password\":\"" + CLAVE_DEMO + "\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.requiereAutenticador").value(true));

            assertTrue(codigoRepository.findAll().stream().anyMatch(codigo -> ADMIN.equals(codigo.getEmail())
                    && CodigoVerificacion.SEGUNDO_FACTOR_CORREO.equals(codigo.getTipo())
                    && !codigo.isUsado()));
        } finally {
            admin.setSegundoFactorCorreoHabilitado(segundoFactorOriginal);
            admin.setAutenticadorHabilitado(autenticadorOriginal);
            admin.setCodigosRespaldoHash(respaldosOriginales);
            usuarioRepository.save(admin);
        }
    }

    @Test
    void recuperacionIndicaCuandoElCorreoNoEstaRegistrado() throws Exception {
        mockMvc.perform(post("/api/auth/solicitar-recuperacion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"correo.inexistente@vetsphere.test\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("No encontramos una cuenta registrada con ese correo."));
    }

    @Test
    void adminPuedeCambiarPasswordYDejarElCampoVacioSinAlterarLaClave() throws Exception {
        var usuario = usuarioRepository.findByEmail(CLIENTE).orElseThrow();
        String hashOriginal = usuario.getPassword();
        String tokenAdmin = tokenDe(ADMIN, CLAVE_DEMO);
        try {
            mockMvc.perform(put("/api/admin/usuarios/" + usuario.getId())
                            .header("Authorization", "Bearer " + tokenAdmin)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"password\":\"\"}"))
                    .andExpect(status().isOk());
            assertTrue(passwordEncoder.matches(CLAVE_DEMO, usuarioRepository.findById(usuario.getId())
                    .orElseThrow().getPassword()), "Un campo vacio debe conservar la contraseña actual");

            mockMvc.perform(put("/api/admin/usuarios/" + usuario.getId())
                            .header("Authorization", "Bearer " + tokenAdmin)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"password\":\"NuevaClave123\"}"))
                    .andExpect(status().isOk());

            String hashNuevo = usuarioRepository.findById(usuario.getId()).orElseThrow().getPassword();
            assertTrue(passwordEncoder.matches("NuevaClave123", hashNuevo));
            assertTrue(!passwordEncoder.matches(CLAVE_DEMO, hashNuevo));
        } finally {
            var usuarioActual = usuarioRepository.findById(usuario.getId()).orElseThrow();
            usuarioActual.setPassword(hashOriginal);
            usuarioRepository.save(usuarioActual);
        }
    }
}
