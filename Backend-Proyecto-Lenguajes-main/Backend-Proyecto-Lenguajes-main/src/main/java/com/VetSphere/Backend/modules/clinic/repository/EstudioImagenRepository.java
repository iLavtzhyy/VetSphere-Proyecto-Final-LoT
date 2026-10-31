package com.VetSphere.Backend.modules.clinic.repository;

import com.VetSphere.Backend.modules.clinic.domain.EstudioImagen;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EstudioImagenRepository extends JpaRepository<EstudioImagen, Long> {
    /** Estudios del paciente ordenados del mas reciente al mas antiguo. */
    List<EstudioImagen> findByMascotaIdOrderByFechaDesc(Long mascotaId);

    /** Todos los estudios ordenados por fecha descendente. */
    List<EstudioImagen> findAllByOrderByFechaDesc();
}
