package com.VetSphere.Backend.modules.clinic.domain;

import jakarta.persistence.*;
import lombok.*;

/**
 * Proveedor real de la clinica (persistido en base de datos).
 * Reemplaza el facade de demostracion que devolvia datos falsos.
 */
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
@Entity
public class Proveedor {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String nombre;
    private String contacto;
    private String telefono;
    private String email;
    private String direccion;
    @Builder.Default
    private boolean activo = true;
}
