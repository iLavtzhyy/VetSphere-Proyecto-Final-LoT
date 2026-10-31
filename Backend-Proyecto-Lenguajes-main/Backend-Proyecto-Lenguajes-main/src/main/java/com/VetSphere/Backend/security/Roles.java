package com.VetSphere.Backend.security;

import java.util.stream.Stream;

/**
 * Utilidad central para normalizar nombres de rol.
 * Evita la duplicacion del metodo normalizarRol() que existia en
 * {@link com.VetSphere.Backend.config.SecurityConfig} y en JwtAuthenticationFilter.
 */
public final class Roles {
    private Roles() {
    }

    /**
     * Normaliza un nombre de rol devolviendo tanto el rol tal cual como su variante con prefijo ROLE_.
     * Ejemplo: "ADMIN" -> ["ADMIN", "ROLE_ADMIN"].
     */
    public static Stream<String> normalizar(String nombre) {
        if (nombre == null || nombre.isBlank()) return Stream.empty();
        String limpio = nombre.trim();
        return limpio.startsWith("ROLE_") ? Stream.of(limpio) : Stream.of(limpio, "ROLE_" + limpio);
    }
}
