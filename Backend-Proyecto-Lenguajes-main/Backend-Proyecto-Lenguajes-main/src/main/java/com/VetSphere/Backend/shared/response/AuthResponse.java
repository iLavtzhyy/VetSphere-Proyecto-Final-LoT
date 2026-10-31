package com.VetSphere.Backend.shared.response;

import java.util.List;

public record AuthResponse(String token, String email, String nombre, List<String> roles,
                           boolean requiereAutenticador, List<String> metodosSegundoFactor) {
    public AuthResponse(String token, String email, String nombre, List<String> roles, boolean requiereAutenticador) {
        this(token, email, nombre, roles, requiereAutenticador, List.of());
    }

    public AuthResponse(String token, String email, String nombre, List<String> roles) {
        this(token, email, nombre, roles, false, List.of());
    }
}


