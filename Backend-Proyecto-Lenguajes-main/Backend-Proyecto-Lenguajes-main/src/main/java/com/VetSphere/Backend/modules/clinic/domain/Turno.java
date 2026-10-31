package com.VetSphere.Backend.modules.clinic.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Turno de trabajo del personal de la clinica (persistido en base de datos).
 * Reemplaza el facade de demostracion que devolvia datos falsos.
 *
 * Un turno pertenece a un empleado y ocupa una franja horaria concreta de un
 * dia. El servicio impide registrar dos turnos solapados para el mismo
 * empleado en la misma fecha.
 */
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
@Entity
public class Turno {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    /** Empleado al que pertenece el turno. */
    @ManyToOne
    @JsonIgnoreProperties({"roles", "password"})
    private Usuario empleado;
    /** Dia en el que se desarrolla el turno. */
    private LocalDate fecha;
    /** Hora de inicio del turno. */
    private LocalTime horaInicio;
    /** Hora de fin del turno (siempre posterior a la de inicio). */
    private LocalTime horaFin;
    /** Area asignada: Consulta, Cirugia, Farmacia, Recepcion, etc. */
    @Column(length = 80)
    private String area;
    /** Observaciones del turno. */
    @Column(length = 600)
    private String notas;
}
