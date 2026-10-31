package com.VetSphere.Backend.modules.clinic.repository;

import com.VetSphere.Backend.modules.clinic.domain.Cirugia;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import java.util.List;

public interface CirugiaRepository extends JpaRepository<Cirugia, Long> {
    List<Cirugia> findByMascotaId(Long mascotaId);
    List<Cirugia> findByFechaProgramadaBetween(LocalDateTime desde, LocalDateTime hasta);

    /** Cirugias vinculadas a una factura (items reales para el PDF). */
    List<Cirugia> findByFacturaId(Long facturaId);
}


