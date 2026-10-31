package com.VetSphere.Backend.modules.clinic.dto;

import com.fasterxml.jackson.annotation.JsonAlias;

/**
 * Mensaje del historial de conversacion que el frontend reenvia al asistente
 * para darle memoria de corto plazo. Solo se aceptan los roles "user" y
 * "assistant"; cualquier otro rol se ignora silenciosamente en el servicio
 * (el historial del cliente NUNCA puede inyectar instrucciones de sistema).
 */
public record MensajeChat(
        @JsonAlias("role")
        String rol,
        @JsonAlias("content")
        String contenido
) {
}
