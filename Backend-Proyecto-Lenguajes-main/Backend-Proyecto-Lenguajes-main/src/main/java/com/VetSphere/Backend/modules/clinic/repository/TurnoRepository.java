package com.VetSphere.Backend.modules.clinic.repository;

import com.VetSphere.Backend.modules.clinic.domain.Turno;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface TurnoRepository extends JpaRepository<Turno, Long> {
    /** Turnos ordenados por fecha y hora de inicio (lectura del cuadrante). */
    List<Turno> findAllByOrderByFechaAscHoraInicioAsc();

    /** Turnos de un empleado en una fecha concreta (deteccion de solapamientos). */
    List<Turno> findByEmpleadoIdAndFecha(Long empleadoId, LocalDate fecha);
}
