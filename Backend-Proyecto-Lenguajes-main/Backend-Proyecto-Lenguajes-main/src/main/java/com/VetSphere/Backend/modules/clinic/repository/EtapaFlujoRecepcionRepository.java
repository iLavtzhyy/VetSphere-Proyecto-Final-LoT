package com.VetSphere.Backend.modules.clinic.repository;

import com.VetSphere.Backend.modules.clinic.domain.EtapaFlujoRecepcion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EtapaFlujoRecepcionRepository extends JpaRepository<EtapaFlujoRecepcion, Long> {
    List<EtapaFlujoRecepcion> findAllByOrderByPosicionAscIdAsc();
    List<EtapaFlujoRecepcion> findByActivaTrueOrderByPosicionAscIdAsc();
    Optional<EtapaFlujoRecepcion> findByNombreIgnoreCase(String nombre);
    Optional<EtapaFlujoRecepcion> findByEstadoCitaIgnoreCase(String estadoCita);
}
