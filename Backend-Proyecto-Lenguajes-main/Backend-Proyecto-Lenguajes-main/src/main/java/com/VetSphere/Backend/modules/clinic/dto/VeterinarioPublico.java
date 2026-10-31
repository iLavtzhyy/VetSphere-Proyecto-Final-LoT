package com.VetSphere.Backend.modules.clinic.dto;

/**
 * Ficha publica del equipo veterinario.
 *
 * Antes se devolvia la entidad {@code Usuario} completa en un endpoint
 * abierto a todo el publico: eso exponia tambien el estado de seguridad de
 * la cuenta (intentos fallidos, bloqueo, motivo del bloqueo, fecha de
 * desactivacion) y los roles internos. Aqui solo se publica lo que aparece
 * en la cartelera del equipo.
 */
public record VeterinarioPublico(
        Long id,
        String nombres,
        String apellidos,
        String especialidad,
        String cedulaProfesional,
        String telefono,
        String email,
        String fotoUrl,
        String biografia
) {
}
