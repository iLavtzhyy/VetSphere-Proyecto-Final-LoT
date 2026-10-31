package com.VetSphere.Backend.modules.clinic.repository;

import com.VetSphere.Backend.modules.clinic.domain.Consentimiento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ConsentimientoRepository extends JpaRepository<Consentimiento, Long> {
    /** Consentimientos ordenados del mas reciente al mas antiguo. */
    List<Consentimiento> findAllByOrderByFechaDesc();

    /** Consentimientos de una mascota. */
    List<Consentimiento> findByMascotaIdOrderByFechaDesc(Long mascotaId);

    /** Consentimientos filtrados por estado. */
    List<Consentimiento> findByEstadoOrderByFechaDesc(String estado);

    /** Consentimientos de una mascota filtrados por estado. */
    List<Consentimiento> findByMascotaIdAndEstadoOrderByFechaDesc(Long mascotaId, String estado);

    /** Verifica que el folio generado no este repetido. */
    boolean existsByFolio(String folio);

    /** Cantidad de consentimientos con el prefijo de folio del ano (para el correlativo). */
    long countByFolioStartingWith(String prefijo);
}
