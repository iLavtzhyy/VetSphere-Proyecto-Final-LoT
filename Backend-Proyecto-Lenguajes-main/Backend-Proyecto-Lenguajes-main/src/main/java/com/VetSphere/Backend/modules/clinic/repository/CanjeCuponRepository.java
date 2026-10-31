package com.VetSphere.Backend.modules.clinic.repository;

import com.VetSphere.Backend.modules.clinic.domain.CanjeCupon;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CanjeCuponRepository extends JpaRepository<CanjeCupon, Long> {
    List<CanjeCupon> findByFacturaId(Long facturaId);
    List<CanjeCupon> findByCuponIdOrderByFechaDesc(Long cuponId);

    /** Numero de canjes registrados de un cupon (limite de usos). */
    long countByCuponId(Long cuponId);
}
