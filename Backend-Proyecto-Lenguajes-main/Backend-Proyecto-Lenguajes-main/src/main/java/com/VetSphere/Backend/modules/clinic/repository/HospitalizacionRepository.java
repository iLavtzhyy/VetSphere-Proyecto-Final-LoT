package com.VetSphere.Backend.modules.clinic.repository;

import com.VetSphere.Backend.modules.clinic.domain.Hospitalizacion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HospitalizacionRepository extends JpaRepository<Hospitalizacion, Long> {
    List<Hospitalizacion> findByMascotaId(Long mascotaId);

    /** Hospitalizaciones vinculadas a una factura (items reales para el PDF). */
    List<Hospitalizacion> findByFacturaId(Long facturaId);
}

