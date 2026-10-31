package com.VetSphere.Backend.modules.clinic.domain;

import jakarta.persistence.*;
import lombok.*;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.LocalDate;

/**
 * Seguimiento clinico de una mascota (postoperatorio, control, vacunacion).
 * La referencia al veterinario es opcional.
 */
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
@Entity
public class Seguimiento {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne
    @JsonIgnoreProperties({"historiales", "cirugias"})
    private Mascota mascota;
    private LocalDate fecha;
    private String indicacion;
    private String tipo;
    private String estado;
    @ManyToOne
    @JsonIgnoreProperties({"roles", "password"})
    private Usuario veterinario;
    @Column(length = 1200)
    private String observaciones;
}
