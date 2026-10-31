package com.VetSphere.Backend.modules.clinic.repository;

import com.VetSphere.Backend.modules.clinic.domain.Seguimiento;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SeguimientoRepository extends JpaRepository<Seguimiento, Long> {
    /** Seguimientos de un tipo concreto (postoperatorio, control, vacunacion). */
    List<Seguimiento> findByTipoIgnoreCaseOrderByFechaAsc(String tipo);

    /** Seguimientos que siguen en estado pendiente, del mas antiguo al mas reciente. */
    List<Seguimiento> findByEstadoIgnoreCaseOrderByFechaAsc(String estado);

    /** Seguimientos de una mascota (mas reciente primero). */
    List<Seguimiento> findByMascotaIdOrderByFechaDesc(Long mascotaId);
}
