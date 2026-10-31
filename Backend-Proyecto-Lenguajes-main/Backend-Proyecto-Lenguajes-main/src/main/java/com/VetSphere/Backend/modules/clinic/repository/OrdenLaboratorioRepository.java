package com.VetSphere.Backend.modules.clinic.repository;

import com.VetSphere.Backend.modules.clinic.domain.OrdenLaboratorio;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrdenLaboratorioRepository extends JpaRepository<OrdenLaboratorio, Long> {
    List<OrdenLaboratorio> findByMascotaId(Long mascotaId);

    /** Ordenes de laboratorio vinculadas a una factura (items reales para el PDF). */
    List<OrdenLaboratorio> findByFacturaId(Long facturaId);
}

