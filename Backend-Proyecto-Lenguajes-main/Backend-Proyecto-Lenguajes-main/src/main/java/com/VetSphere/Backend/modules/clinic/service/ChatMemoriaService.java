package com.VetSphere.Backend.modules.clinic.service;

import com.VetSphere.Backend.exception.ResourceNotFoundException;
import com.VetSphere.Backend.modules.clinic.domain.ChatConversacion;
import com.VetSphere.Backend.modules.clinic.domain.ChatMensaje;
import com.VetSphere.Backend.modules.clinic.domain.Usuario;
import com.VetSphere.Backend.modules.clinic.dto.*;
import com.VetSphere.Backend.modules.clinic.repository.ChatConversacionRepository;
import com.VetSphere.Backend.modules.clinic.repository.ChatMensajeRepository;
import com.VetSphere.Backend.modules.clinic.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ChatMemoriaService {
    private final ChatService chatService;
    private final UsuarioRepository usuarioRepository;
    private final ChatConversacionRepository conversacionRepository;
    private final ChatMensajeRepository mensajeRepository;

    public ChatResponse preguntar(String email, String pregunta, Long conversacionId) {
        Usuario usuario = usuario(email);
        ChatConversacion conversacion = obtenerConversacion(usuario, conversacionId, pregunta);

        List<MensajeChat> contexto = mensajeRepository.findTop24ByConversacion_IdOrderByFechaDescIdDesc(conversacion.getId())
                .stream().map(m -> new MensajeChat(m.getRol(), m.getContenido())).toList();
        List<MensajeChat> ordenado = new java.util.ArrayList<>(contexto);
        Collections.reverse(ordenado);

        ChatResponse respuesta = chatService.preguntar(pregunta, ordenado);
        LocalDateTime ahora = LocalDateTime.now();
        mensajeRepository.save(ChatMensaje.builder().conversacion(conversacion).rol("user").contenido(pregunta).fecha(ahora).build());
        mensajeRepository.save(ChatMensaje.builder().conversacion(conversacion).rol("assistant")
                .contenido(respuesta.respuesta()).fecha(ahora.plusNanos(1)).build());
        conversacion.setActualizadoEn(ahora);
        conversacionRepository.save(conversacion);
        return new ChatResponse(respuesta.respuesta(), respuesta.consultoBaseDatos(),
                respuesta.asistenteConfigurado(), conversacion.getId());
    }

    public Long guardarTurnoLocal(String email, String pregunta, String respuesta, Long conversacionId) {
        Usuario usuario = usuario(email);
        ChatConversacion conversacion = obtenerConversacion(usuario, conversacionId, pregunta);
        LocalDateTime ahora = LocalDateTime.now();
        mensajeRepository.save(ChatMensaje.builder().conversacion(conversacion).rol("user").contenido(pregunta).fecha(ahora).build());
        mensajeRepository.save(ChatMensaje.builder().conversacion(conversacion).rol("assistant").contenido(respuesta).fecha(ahora.plusNanos(1)).build());
        conversacion.setActualizadoEn(ahora);
        conversacionRepository.save(conversacion);
        return conversacion.getId();
    }

    public ChatConversacionDetalle actual(String email) {
        Usuario usuario = usuario(email);
        return conversacionRepository.findTopByUsuario_IdOrderByActualizadoEnDesc(usuario.getId())
                .map(this::detalle).orElse(null);
    }

    public List<ChatConversacionResumen> listarTodas() {
        return conversacionRepository.findAllByOrderByActualizadoEnDesc(PageRequest.of(0, 100)).stream()
                .map(c -> new ChatConversacionResumen(c.getId(), c.getUsuario().getEmail(),
                        nombre(c.getUsuario()), c.getTitulo(), c.getActualizadoEn(),
                        mensajeRepository.countByConversacion_Id(c.getId())))
                .toList();
    }

    @Transactional
    public void eliminarConversacionAdmin(Long conversacionId) {
        conversacionRepository.findById(conversacionId)
                .orElseThrow(() -> new ResourceNotFoundException("La conversación no existe."));
        mensajeRepository.deleteByConversacion_Id(conversacionId);
        conversacionRepository.eliminarPorId(conversacionId);
    }

    @Transactional
    public int eliminarTodoElHistorialAdmin() {
        int total = Math.toIntExact(conversacionRepository.count());
        // Borrar primero los mensajes evita la FK; los borrados por lote evitan
        // una consulta y un commit por cada conversación del historial.
        mensajeRepository.deleteAllInBatch();
        conversacionRepository.deleteAllInBatch();
        return total;
    }

    @Transactional
    public void eliminarHistorialUsuario(Long usuarioId) {
        for (ChatConversacion conversacion : conversacionRepository.findAllByUsuario_Id(usuarioId)) {
            mensajeRepository.deleteByConversacion_Id(conversacion.getId());
        }
        conversacionRepository.deleteByUsuario_Id(usuarioId);
    }

    public ChatConversacionDetalle detalleAdmin(Long conversacionId) {
        ChatConversacion conversacion = conversacionRepository.findById(conversacionId)
                .orElseThrow(() -> new ResourceNotFoundException("La conversación no existe."));
        return detalle(conversacion);
    }

    private ChatConversacionDetalle detalle(ChatConversacion conversacion) {
        List<ChatMensaje> recientes = new java.util.ArrayList<>(mensajeRepository
                .findTop200ByConversacion_IdOrderByFechaDescIdDesc(conversacion.getId()));
        Collections.reverse(recientes);
        List<ChatMensajeVista> mensajes = recientes.stream()
                .map(m -> new ChatMensajeVista(m.getRol(), m.getContenido(), m.getFecha())).toList();
        return new ChatConversacionDetalle(conversacion.getId(), conversacion.getUsuario().getEmail(),
                nombre(conversacion.getUsuario()), conversacion.getTitulo(), conversacion.getActualizadoEn(), mensajes);
    }

    private Usuario usuario(String email) {
        return usuarioRepository.findByEmail(AuthService.normalizar(email))
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado."));
    }

    private ChatConversacion obtenerConversacion(Usuario usuario, Long conversacionId, String pregunta) {
        ChatConversacion conversacion = conversacionId == null
                ? conversacionRepository.findTopByUsuario_IdOrderByActualizadoEnDesc(usuario.getId()).orElse(null)
                : conversacionRepository.findByIdAndUsuario_Id(conversacionId, usuario.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("La conversación no existe."));
        if (conversacion == null) {
            LocalDateTime ahora = LocalDateTime.now();
            conversacion = conversacionRepository.save(ChatConversacion.builder()
                    .usuario(usuario).titulo(titulo(pregunta)).creadoEn(ahora).actualizadoEn(ahora).build());
        }
        return conversacion;
    }

    private static String titulo(String pregunta) {
        String texto = pregunta == null ? "Conversación" : pregunta.trim().replaceAll("\\s+", " ");
        return texto.length() <= 200 ? texto : texto.substring(0, 197) + "...";
    }

    private static String nombre(Usuario usuario) {
        return ((usuario.getNombres() == null ? "" : usuario.getNombres()) + " "
                + (usuario.getApellidos() == null ? "" : usuario.getApellidos())).trim();
    }
}
