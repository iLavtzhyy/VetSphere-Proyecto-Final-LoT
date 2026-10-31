package com.VetSphere.Backend.modules.clinic.repository;

import com.VetSphere.Backend.modules.clinic.domain.AuditorEvento;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface AuditorEventoRepository extends JpaRepository<AuditorEvento, Long> {

    List<AuditorEvento> findTop200ByOrderByFechaDesc();

    @Query("select e from AuditorEvento e order by e.fecha desc, e.id desc")
    Page<AuditorEvento> listarPaginado(Pageable pageable);
}
