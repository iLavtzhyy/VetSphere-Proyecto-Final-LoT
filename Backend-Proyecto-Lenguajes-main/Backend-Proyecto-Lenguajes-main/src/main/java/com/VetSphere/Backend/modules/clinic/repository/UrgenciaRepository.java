package com.VetSphere.Backend.modules.clinic.repository;

import com.VetSphere.Backend.modules.clinic.domain.Urgencia;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface UrgenciaRepository extends JpaRepository<Urgencia, Long> {
    /** Urgencias en un estado concreto, ordenadas por hora de ingreso. */
    List<Urgencia> findByEstadoIgnoreCaseOrderByHoraIngresoAsc(String estado);

    /** Urgencias de una mascota (mas reciente primero). */
    List<Urgencia> findByMascotaIdOrderByHoraIngresoDesc(Long mascotaId);
}
