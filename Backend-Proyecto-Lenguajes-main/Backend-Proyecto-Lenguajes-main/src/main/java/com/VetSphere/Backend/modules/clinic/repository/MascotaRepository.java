package com.VetSphere.Backend.modules.clinic.repository;

import com.VetSphere.Backend.modules.clinic.domain.Mascota;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface MascotaRepository extends JpaRepository<Mascota, Long> {
    List<Mascota> findByPropietarioId(Long propietarioId);
    List<Mascota> findByPropietarioEmail(String email);
    long countByEstado(String estado);
}


