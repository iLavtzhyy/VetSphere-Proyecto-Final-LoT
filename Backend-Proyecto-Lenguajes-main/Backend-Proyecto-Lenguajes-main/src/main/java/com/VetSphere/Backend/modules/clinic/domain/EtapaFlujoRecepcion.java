package com.VetSphere.Backend.modules.clinic.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "etapas_flujo_recepcion")
public class EtapaFlujoRecepcion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 60)
    private String nombre;

    @Column(name = "estado_cita", nullable = false, unique = true, length = 60)
    private String estadoCita;

    @Column(length = 180)
    private String descripcion;

    @Column(nullable = false, length = 20)
    private String color;

    @Column(nullable = false)
    private Integer posicion;

    @Column(nullable = false)
    private boolean activa;
}
