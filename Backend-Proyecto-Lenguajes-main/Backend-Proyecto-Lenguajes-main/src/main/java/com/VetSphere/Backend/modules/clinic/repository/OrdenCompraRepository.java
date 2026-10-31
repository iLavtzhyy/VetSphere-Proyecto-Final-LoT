package com.VetSphere.Backend.modules.clinic.repository;

import com.VetSphere.Backend.modules.clinic.domain.OrdenCompra;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrdenCompraRepository extends JpaRepository<OrdenCompra, Long> {
    /** Ordenes de compra ordenadas por fecha descendente (la mas reciente primero). */
    List<OrdenCompra> findAllByOrderByFechaDesc();

    /** Indica si ya existe una orden registrada con ese numero. */
    boolean existsByNumero(String numero);

    /** Ordenes cuyo numero empieza por el prefijo indicado (calculo del consecutivo). */
    List<OrdenCompra> findByNumeroStartingWith(String prefijo);
}
