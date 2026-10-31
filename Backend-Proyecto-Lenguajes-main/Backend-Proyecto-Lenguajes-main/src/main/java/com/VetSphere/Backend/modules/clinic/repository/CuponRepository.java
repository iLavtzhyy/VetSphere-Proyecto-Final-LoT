package com.VetSphere.Backend.modules.clinic.repository;

import com.VetSphere.Backend.modules.clinic.domain.Cupon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface CuponRepository extends JpaRepository<Cupon, Long> {

    Optional<Cupon> findByCodigoIgnoreCase(String codigo);

    List<Cupon> findByActivoTrueOrderByFechaCreacionDesc();

    List<Cupon> findAllByOrderByFechaCreacionDesc();

    /** Cupones vigentes en la fecha indicada (para validar durante la venta). */
    @Query("""
            select c from Cupon c
            where c.activo = true
              and c.fechaInicio <= :momento
              and c.fechaFin >= :momento
            """)
    List<Cupon> findVigentes(@Param("momento") LocalDateTime momento);

    }
