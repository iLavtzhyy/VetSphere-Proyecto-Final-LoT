package com.VetSphere.Backend.modules.clinic.repository;

import com.VetSphere.Backend.modules.clinic.domain.HistorialClinico;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface HistorialClinicoRepository extends JpaRepository<HistorialClinico, Long> {
    List<HistorialClinico> findByMascotaIdOrderByFechaDesc(Long mascotaId);

    /** Consultas vinculadas a una factura (items reales para el PDF). */
    List<HistorialClinico> findByFacturaId(Long facturaId);
}


