package com.VetSphere.Backend.modules.clinic.domain;

import jakarta.persistence.*;
import lombok.*;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.LocalDateTime;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
@Entity
public class Cirugia {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private LocalDateTime fechaProgramada;
    private String procedimiento;
    private String tipoAnestesia;
    private String cirujanoPrincipal;
    private String constantesVitales;
    private String estadoPostoperatorio;
    private String estado;
    @Column(length = 1400)
    private String notas;
    @Builder.Default
    private java.math.BigDecimal precio = java.math.BigDecimal.valueOf(280.00);
    private Long facturaId;
    @ManyToOne
    @JsonIgnoreProperties({"historiales", "cirugias"})
    private Mascota mascota;
    @ManyToOne
    @JsonIgnoreProperties({"roles", "password"})
    private Usuario cirujano;
}


