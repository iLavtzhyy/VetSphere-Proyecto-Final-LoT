package com.VetSphere.Backend.modules.clinic.domain;

import jakarta.persistence.*;
import lombok.*;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.LocalDateTime;

/**
 * Ingreso por urgencia de una mascota con su prioridad de atencion.
 * La referencia al veterinario es opcional.
 */
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
@Entity
public class Urgencia {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne
    @JsonIgnoreProperties({"historiales", "cirugias"})
    private Mascota mascota;
    private LocalDateTime horaIngreso;
    private String prioridad;
    private String motivo;
    private String estado;
    @ManyToOne
    @JsonIgnoreProperties({"roles", "password"})
    private Usuario veterinario;
    @Column(length = 1200)
    private String notas;
}
