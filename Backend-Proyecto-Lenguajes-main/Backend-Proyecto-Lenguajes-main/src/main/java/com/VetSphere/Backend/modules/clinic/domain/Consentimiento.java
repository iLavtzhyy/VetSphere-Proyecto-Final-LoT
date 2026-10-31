package com.VetSphere.Backend.modules.clinic.domain;

import jakarta.persistence.*;
import lombok.*;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.LocalDateTime;

/**
 * Consentimiento firmado por el propietario de la mascota. Puede estar
 * vinculado a una cirugia (consentimiento quirurgico) o ser un consentimiento
 * de tratamiento o de vacunacion.
 */
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
@Entity
public class Consentimiento {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String folio;
    @ManyToOne
    @JsonIgnoreProperties({"historiales", "cirugias"})
    private Mascota mascota;
    @ManyToOne
    @JsonIgnoreProperties({"historiales", "cirugias"})
    private Cirugia cirugia;
    private String tipo;
    private String firmadoPor;
    private String cargo;
    private LocalDateTime fecha;
    private String estado;
    @Column(length = 4000)
    private String documento;
    @Column(length = 1000)
    private String observaciones;
}
