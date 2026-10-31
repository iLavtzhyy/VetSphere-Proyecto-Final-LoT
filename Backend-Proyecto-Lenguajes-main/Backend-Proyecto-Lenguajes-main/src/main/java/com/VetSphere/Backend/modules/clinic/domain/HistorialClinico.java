package com.VetSphere.Backend.modules.clinic.domain;

import jakarta.persistence.*;
import lombok.*;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.LocalDateTime;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
@Entity
public class HistorialClinico {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private LocalDateTime fecha;
    private String tipoEvento;
    private String motivo;
    @Column(length = 1800)
    private String diagnostico;
    @Column(length = 1800)
    private String tratamiento;
    private String vacunas;
    private String archivosUrl;
    private Double temperatura;
    private Integer frecuenciaCardiaca;
    private Integer frecuenciaRespiratoria;
    @Builder.Default
    private java.math.BigDecimal precio = java.math.BigDecimal.valueOf(45.00);
    private Long facturaId;
    @ManyToOne
    @JsonIgnoreProperties({"historiales", "cirugias"})
    private Mascota mascota;
    @ManyToOne
    @JsonIgnoreProperties({"roles", "password"})
    private Usuario veterinario;
}


