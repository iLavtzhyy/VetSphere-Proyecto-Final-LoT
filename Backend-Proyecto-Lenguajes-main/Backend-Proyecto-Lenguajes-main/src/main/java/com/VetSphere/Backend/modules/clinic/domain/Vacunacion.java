package com.VetSphere.Backend.modules.clinic.domain;

import jakarta.persistence.*;
import lombok.*;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.LocalDate;

/**
 * Registro de una vacunacion aplicada o programada para una mascota.
 * Las columnas de referencia (mascota) pueden quedar nulas: la regla del
 * proyecto es que las columnas nuevas nunca son obligatorias.
 */
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
@Entity
public class Vacunacion {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne
    @JsonIgnoreProperties({"historiales", "cirugias"})
    private Mascota mascota;
    private String vacuna;
    private LocalDate fecha;
    private LocalDate fechaProxima;
    private String lote;
    private String estado;
    @Column(length = 1200)
    private String observaciones;
}
