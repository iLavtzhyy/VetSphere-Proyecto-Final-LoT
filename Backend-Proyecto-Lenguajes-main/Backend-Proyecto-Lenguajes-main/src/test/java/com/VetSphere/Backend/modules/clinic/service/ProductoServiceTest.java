package com.VetSphere.Backend.modules.clinic.service;

import com.VetSphere.Backend.exception.BusinessException;
import com.VetSphere.Backend.exception.ResourceNotFoundException;
import com.VetSphere.Backend.modules.clinic.domain.MovimientoInventario;
import com.VetSphere.Backend.modules.clinic.domain.Producto;
import com.VetSphere.Backend.modules.clinic.repository.MovimientoInventarioRepository;
import com.VetSphere.Backend.modules.clinic.repository.ProductoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias del servicio de inventario de la farmacia:
 * validaciones de stock, movimientos persistentes y errores tipados.
 */
class ProductoServiceTest {

    private ProductoRepository productoRepository;
    private MovimientoInventarioRepository movimientoRepository;
    private AuditoriaService auditoriaService;
    private ProductoService productoService;

    @BeforeEach
    void configurar() {
        productoRepository = mock(ProductoRepository.class);
        movimientoRepository = mock(MovimientoInventarioRepository.class);
        auditoriaService = mock(AuditoriaService.class);
        productoService = new ProductoService(productoRepository, movimientoRepository, auditoriaService);
    }

    private Producto productoConStock(int stock) {
        return Producto.builder()
                .id(1L).codigo("MED-001").nombre("Amoxicilina").categoria("Antibiotico")
                .stock(stock).stockCritico(5).precio(BigDecimal.valueOf(12.50))
                .build();
    }

    @Test
    void descontarStockSinStockSuficienteLanzaBusinessException() {
        when(productoRepository.findByCodigoParaActualizar("MED-001"))
                .thenReturn(Optional.of(productoConStock(3)));

        BusinessException excepcion = assertThrows(BusinessException.class,
                () -> productoService.descontarStock("MED-001", 10));

        assertTrue(excepcion.getMessage().toLowerCase().contains("stock insuficiente"));
        verify(movimientoRepository, never()).save(any());
    }

    @Test
    void descontarStockDescuentaYRegistraElMovimiento() {
        Producto producto = productoConStock(10);
        when(productoRepository.findByCodigoParaActualizar("MED-001")).thenReturn(Optional.of(producto));
        when(productoRepository.save(any(Producto.class))).thenAnswer(inv -> inv.getArgument(0));

        Producto resultado = productoService.descontarStock("MED-001", 4);

        assertEquals(6, resultado.getStock());
        ArgumentCaptor<MovimientoInventario> captor = ArgumentCaptor.forClass(MovimientoInventario.class);
        verify(movimientoRepository).save(captor.capture());
        assertEquals(MovimientoInventario.SALIDA, captor.getValue().getTipo());
        assertEquals(4, captor.getValue().getCantidad());
        assertEquals(10, captor.getValue().getStockAnterior());
        assertEquals(6, captor.getValue().getStockNuevo());
    }

    @Test
    void agregarStockConCantidadNoPositivaLanzaBusinessException() {
        assertThrows(BusinessException.class, () -> productoService.agregarStock("MED-001", 0));
        assertThrows(BusinessException.class, () -> productoService.agregarStock("MED-001", -3));
    }

    @Test
    void agregarStockSumaYRegistraEntrada() {
        Producto producto = productoConStock(2);
        when(productoRepository.findByCodigoParaActualizar("MED-001")).thenReturn(Optional.of(producto));
        when(productoRepository.save(any(Producto.class))).thenAnswer(inv -> inv.getArgument(0));

        Producto resultado = productoService.agregarStock("MED-001", 8);

        assertEquals(10, resultado.getStock());
        ArgumentCaptor<MovimientoInventario> captor = ArgumentCaptor.forClass(MovimientoInventario.class);
        verify(movimientoRepository).save(captor.capture());
        assertEquals(MovimientoInventario.ENTRADA, captor.getValue().getTipo());
    }

    @Test
    void consultarProductoInexistenteLanzaResourceNotFound() {
        when(productoRepository.findByCodigo("NO-EXISTE")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> productoService.consultar("NO-EXISTE"));
    }
}
