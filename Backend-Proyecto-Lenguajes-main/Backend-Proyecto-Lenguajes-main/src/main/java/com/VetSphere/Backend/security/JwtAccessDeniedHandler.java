package com.VetSphere.Backend.security;

import com.VetSphere.Backend.shared.response.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Devuelve 403 con el envoltorio {@link ApiResponse} cuando un usuario autenticado
 * intenta acceder a un recurso para el que no tiene el rol adecuado. Sin este
 * handler Spring Security responde con un cuerpo vacio (sendError), lo que romperia
 * el contrato de envoltorio que el frontend espera en toda respuesta.
 */
@Component
@RequiredArgsConstructor
public class JwtAccessDeniedHandler implements AccessDeniedHandler {
    private final ObjectMapper objectMapper;

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        String mensaje = "Acceso denegado: tu rol no tiene permisos para este recurso.";
        objectMapper.writeValue(response.getWriter(), ApiResponse.error(mensaje));
    }
}
