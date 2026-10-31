package com.VetSphere.Backend.modules.clinic.repository;

import com.VetSphere.Backend.modules.clinic.domain.ConsumoHospitalizacion;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ConsumoHospitalizacionRepository extends JpaRepository<ConsumoHospitalizacion, Long> {
    List<ConsumoHospitalizacion> findByHospitalizacionIdOrderByFechaAsc(Long hospitalizacionId);
}
