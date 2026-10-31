package com.VetSphere.Backend.modules.clinic.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
@Entity
public class Cita {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private LocalDateTime fechaHora;
    private String motivo;
    private String estado;
    private String observaciones;
    @ManyToOne private Mascota mascota;
    @ManyToOne private Usuario veterinario;
    @ManyToOne private Usuario recepcionista;
}

