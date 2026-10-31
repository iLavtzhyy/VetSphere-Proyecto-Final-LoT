package com.VetSphere.Backend.modules.clinic.controller;

import com.VetSphere.Backend.modules.clinic.dto.ChatRequest;
import com.VetSphere.Backend.modules.clinic.dto.ChatResponse;
import com.VetSphere.Backend.modules.clinic.service.ChatMemoriaService;
import com.VetSphere.Backend.shared.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.security.core.Authentication;

import java.util.List;
import com.VetSphere.Backend.modules.clinic.dto.ChatConversacionDetalle;
import com.VetSphere.Backend.modules.clinic.dto.ChatConversacionResumen;
import com.VetSphere.Backend.modules.clinic.dto.ChatTurnoLocalRequest;

/**
 * Asistente inteligente de VetSphere. Cualquier usuario autenticado puede
 * consultarlo; el backend decide que datos puede tocar el modelo.
 */
@Tag(name = "Chat", description = "Asistente inteligente basado en IA")
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {
    private final ChatMemoriaService chatMemoriaService;

    @PostMapping
    @Operation(summary = "Pregunta al asistente inteligente (usa herramientas de solo lectura sobre la base)")
    public ApiResponse<ChatResponse> preguntar(@Valid @RequestBody ChatRequest request, Authentication authentication) {
        return ApiResponse.ok("Consulta procesada", chatMemoriaService.preguntar(authentication.getName(),
                request.pregunta(), request.conversacionId()));
    }

    @PostMapping("/local")
    @Operation(summary = "Guarda un turno determinístico del asistente en la memoria de la conversación")
    public ApiResponse<Long> guardarTurnoLocal(@Valid @RequestBody ChatTurnoLocalRequest request,
                                                Authentication authentication) {
        return ApiResponse.ok("Conversación guardada", chatMemoriaService.guardarTurnoLocal(
                authentication.getName(), request.pregunta(), request.respuesta(), request.conversacionId()));
    }

    @GetMapping("/conversacion-actual")
    @Operation(summary = "Recupera la conversación reciente del usuario autenticado")
    public ApiResponse<ChatConversacionDetalle> conversacionActual(Authentication authentication) {
        return ApiResponse.ok("Conversación recuperada", chatMemoriaService.actual(authentication.getName()));
    }

    @GetMapping("/admin/conversaciones")
    @Operation(summary = "Lista conversaciones recientes para el panel de administración")
    public ApiResponse<List<ChatConversacionResumen>> conversacionesAdmin() {
        return ApiResponse.ok("Historial de chat", chatMemoriaService.listarTodas());
    }

    @GetMapping("/admin/conversaciones/{id}")
    @Operation(summary = "Consulta todos los mensajes de una conversación")
    public ApiResponse<ChatConversacionDetalle> conversacionAdmin(@PathVariable Long id) {
        return ApiResponse.ok("Conversación recuperada", chatMemoriaService.detalleAdmin(id));
    }

    @DeleteMapping("/admin/conversaciones/{id}")
    @Operation(summary = "Elimina una conversación del historial administrativo")
    public ApiResponse<Void> eliminarConversacionAdmin(@PathVariable Long id) {
        chatMemoriaService.eliminarConversacionAdmin(id);
        return ApiResponse.ok("Conversación eliminada", null);
    }

    @PostMapping("/admin/conversaciones/{id}/eliminar")
    @Operation(summary = "Elimina una conversación del historial administrativo")
    public ApiResponse<Void> eliminarConversacionAdminCompatible(@PathVariable Long id) {
        chatMemoriaService.eliminarConversacionAdmin(id);
        return ApiResponse.ok("Conversación eliminada", null);
    }

    @DeleteMapping("/admin/conversaciones")
    @Operation(summary = "Elimina todo el historial de conversaciones")
    public ApiResponse<Integer> eliminarHistorialAdmin() {
        return ApiResponse.ok("Historial eliminado", chatMemoriaService.eliminarTodoElHistorialAdmin());
    }

    @PostMapping("/admin/conversaciones/eliminar-todo")
    @Operation(summary = "Elimina todo el historial de conversaciones")
    public ApiResponse<Integer> eliminarHistorialAdminCompatible() {
        return ApiResponse.ok("Historial eliminado", chatMemoriaService.eliminarTodoElHistorialAdmin());
    }
}
