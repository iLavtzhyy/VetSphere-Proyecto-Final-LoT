package com.VetSphere.Backend.modules.clinic.domain;

import jakarta.persistence.*;
import lombok.*;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.LocalDate;

/**
 * Estudio de imagenologia (radiografia, ecografia, TAC, resonancia) solicitado
 * para una mascota. La referencia a la mascota y al veterinario es opcional
 * para no romper registros historicos migrados.
 */
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
@Entity
public class EstudioImagen {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne
    @JsonIgnoreProperties({"historiales", "cirugias"})
    private Mascota mascota;
    private String tipo;
    private LocalDate fecha;
    private String estado;
    @Column(length = 1800)
    private String informe;
    @ManyToOne
    @JsonIgnoreProperties({"roles", "password"})
    private Usuario veterinario;
    private String archivoUrl;
}
