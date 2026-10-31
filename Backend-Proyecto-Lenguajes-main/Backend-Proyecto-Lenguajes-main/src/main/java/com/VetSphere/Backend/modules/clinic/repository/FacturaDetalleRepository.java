package com.VetSphere.Backend.modules.clinic.repository;

import com.VetSphere.Backend.modules.clinic.domain.FacturaDetalle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface FacturaDetalleRepository extends JpaRepository<FacturaDetalle, Long> {

    List<FacturaDetalle> findByFacturaIdOrderByIdAsc(Long facturaId);

    Optional<FacturaDetalle> findFirstByFacturaIdAndProductoId(Long facturaId, Long productoId);

    /**
     * Ventas agregadas por producto en una ventana temporal.
     * Devuelve: [productoId, unidades, ingresos, ultimaVenta].
     */
    @Query("""
            select d.producto.id, sum(d.cantidad), sum(d.subtotal), max(d.factura.fecha)
            from FacturaDetalle d
            where d.producto is not null
              and d.factura.fecha >= :desde
              and (d.factura.estadoPago is null or d.factura.estadoPago <> 'Anulado')
            group by d.producto.id
            """)
    List<Object[]> ventasPorProducto(@Param("desde") LocalDateTime desde);

    /** Ultima venta registrada de cada producto (historico completo). */
    @Query("""
            select d.producto.id, max(d.factura.fecha)
            from FacturaDetalle d
            where d.producto is not null
              and (d.factura.estadoPago is null or d.factura.estadoPago <> 'Anulado')
            group by d.producto.id
            """)
    List<Object[]> ultimaVentaPorProducto();

    /** Unidades vendidas por producto en toda la historia (para clasificar rotacion). */
    @Query("""
            select d.producto.id, sum(d.cantidad)
            from FacturaDetalle d
            where d.producto is not null
              and (d.factura.estadoPago is null or d.factura.estadoPago <> 'Anulado')
            group by d.producto.id
            """)
    List<Object[]> unidadesTotalesPorProducto();
}
