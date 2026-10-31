package com.VetSphere.Backend.modules.clinic.repository;

import com.VetSphere.Backend.modules.clinic.domain.Vacunacion;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;

public interface VacunacionRepository extends JpaRepository<Vacunacion, Long> {
    /** Vacunaciones programadas para hoy o hacia adelante, en orden cronologico. */
    List<Vacunacion> findByFechaGreaterThanEqualOrderByFechaAsc(LocalDate fecha);

    /** Historial de vacunaciones de una mascota (mas reciente primero). */
    List<Vacunacion> findByMascotaIdOrderByFechaDesc(Long mascotaId);
}
