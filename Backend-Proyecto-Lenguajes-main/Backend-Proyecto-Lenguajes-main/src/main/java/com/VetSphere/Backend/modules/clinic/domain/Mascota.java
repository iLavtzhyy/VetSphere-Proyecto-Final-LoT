package com.VetSphere.Backend.modules.clinic.domain;

import jakarta.persistence.*;
import lombok.*;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
@Entity
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Mascota {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String nombre;
    private String especie;
    private String raza;
    private String sexo;
    private String color;
    private Double pesoKg;
    private LocalDate fechaNacimiento;
    private String fotoUrl;
    private String estado;
    @ManyToOne
    @JsonIgnoreProperties({"roles", "password"})
    private Usuario propietario;
    @OneToMany(mappedBy = "mascota", fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("fecha DESC")
    @Builder.Default
    private List<HistorialClinico> historiales = new ArrayList<>();
    @OneToMany(mappedBy = "mascota", fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("fechaProgramada DESC")
    @Builder.Default
    private List<Cirugia> cirugias = new ArrayList<>();
}


