package com.VetSphere.Backend.modules.clinic.dto;

/**
 * Respuesta del asistente inteligente.
 *
 * @param respuesta           texto que se muestra al usuario (nunca expone detalles internos)
 * @param consultoBaseDatos   true si se ejecutaron herramientas contra la base de datos
 * @param asistenteConfigurado false cuando falta la clave de API del proveedor de IA
 */
public record ChatResponse(
        String respuesta,
        boolean consultoBaseDatos,
        boolean asistenteConfigurado,
        Long conversacionId
) {
    public ChatResponse(String respuesta, boolean consultoBaseDatos, boolean asistenteConfigurado) {
        this(respuesta, consultoBaseDatos, asistenteConfigurado, null);
    }
}
