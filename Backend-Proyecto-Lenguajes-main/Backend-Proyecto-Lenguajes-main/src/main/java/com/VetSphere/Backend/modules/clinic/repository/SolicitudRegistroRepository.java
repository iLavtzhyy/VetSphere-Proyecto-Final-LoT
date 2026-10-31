package com.VetSphere.Backend.modules.clinic.repository;

import com.VetSphere.Backend.modules.clinic.domain.SolicitudRegistro;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface SolicitudRegistroRepository extends JpaRepository<SolicitudRegistro, Long> {
    Optional<SolicitudRegistro> findByEmail(String email);
}
