package com.VetSphere.Backend.modules.clinic.service;

import com.VetSphere.Backend.exception.BusinessException;
import com.VetSphere.Backend.modules.clinic.domain.Cupon;
import com.VetSphere.Backend.modules.clinic.domain.Factura;
import com.VetSphere.Backend.modules.clinic.domain.HistorialClinico;
import com.VetSphere.Backend.modules.clinic.domain.Producto;
import com.VetSphere.Backend.modules.clinic.dto.FacturaRequest;
import com.VetSphere.Backend.modules.clinic.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias del recalculo server-side de facturas:
 * el backend nunca confia en los totales enviados por el cliente, resuelve los
 * precios contra el catalogo y aplica los cupones en el servidor.
 */
class FacturacionServiceTest {

    private FacturaRepository facturaRepository;
    private FacturaDetalleRepository detalleRepository;
    private HistorialClinicoRepository historialRepository;
    private ProductoRepository productoRepository;
    private ProductoService productoService;
    private CuponService cuponService;
    private FacturacionService facturacionService;

    @BeforeEach
    void configurar() {
        facturaRepository = mock(FacturaRepository.class);
        detalleRepository = mock(FacturaDetalleRepository.class);
        historialRepository = mock(HistorialClinicoRepository.class);
        CirugiaRepository cirugiaRepository = mock(CirugiaRepository.class);
        OrdenLaboratorioRepository ordenRepository = mock(OrdenLaboratorioRepository.class);
        HospitalizacionRepository hospitalizacionRepository = mock(HospitalizacionRepository.class);
        UsuarioRepository usuarioRepository = mock(UsuarioRepository.class);
        MascotaRepository mascotaRepository = mock(MascotaRepository.class);
        productoRepository = mock(ProductoRepository.class);
        productoService = mock(ProductoService.class);
        cuponService = mock(CuponService.class);
        AuditoriaService auditoriaService = mock(AuditoriaService.class);

        TarifaService tarifaService = new TarifaService(
                new BigDecimal("45.00"), new BigDecimal("280.00"), new BigDecimal("35.00"),
                new BigDecimal("45.00"), new BigDecimal("120.00"), new BigDecimal("35.00"),
                new BigDecimal("65.00"));

        facturacionService = new FacturacionService(facturaRepository, detalleRepository,
                historialRepository, cirugiaRepository, ordenRepository, hospitalizacionRepository,
                usuarioRepository, mascotaRepository, productoRepository, productoService,
                cuponService, tarifaService, auditoriaService);

        when(facturaRepository.save(any(Factura.class))).thenAnswer(inv -> {
            Factura factura = inv.getArgument(0);
            if (factura.getId() == null) factura.setId(99L);
            return factura;
        });
        when(facturaRepository.count()).thenReturn(0L);
        // Sin cupon solicitado el servicio devuelve un descuento vacio.
        when(cuponService.validarYCalcular(any(), any()))
                .thenReturn(CuponService.AplicacionCupon.vacio());
    }

    private FacturaRequest requestDirecto(String concepto, BigDecimal subtotal, BigDecimal totalCliente) {
        return new FacturaRequest(null, null, concepto, subtotal, null, totalCliente,
                null, null, null, null, null, null, null, null, null, null, null);
    }

    @Test
    void recalcularImpuestosYTotalIgnoringLosDelCliente() {
        // El cliente manda total=1 y subtotal=100; el backend debe calcular IVA 15% y total real.
        Factura factura = facturacionService.crear(requestDirecto("Venta de antipulgas", new BigDecimal("100.00"), new BigDecimal("1.00")));

        assertEquals(new BigDecimal("100.00"), factura.getSubtotal());
        assertEquals(new BigDecimal("15.00"), factura.getImpuestos());
        assertEquals(new BigDecimal("115.00"), factura.getTotal());
        assertEquals("Pendiente", factura.getEstadoPago());
    }

    @Test
    void subtotalInvalidoLanzaBusinessException() {
        assertThrows(BusinessException.class,
                () -> facturacionService.crear(requestDirecto("Sin subtotal", null, null)));
        assertThrows(BusinessException.class,
                () -> facturacionService.crear(requestDirecto("Subtotal negativo", new BigDecimal("-5.00"), null)));
    }

    @Test
    void facturaConServiciosUsaElPrecioDeLaBaseYVinculaElServicio() {
        HistorialClinico consulta = HistorialClinico.builder()
                .id(10L)
                .precio(new BigDecimal("45.00"))
                .facturaId(null)
                .build();
        when(historialRepository.findById(10L)).thenReturn(Optional.of(consulta));

        FacturaRequest request = new FacturaRequest(null, null, "Consulta general", null, null, null,
                null, null, null, null, List.of(10L), null, null, null, null, null, null);

        Factura factura = facturacionService.crear(request);

        assertEquals(new BigDecimal("45.00"), factura.getSubtotal());
        assertEquals(new BigDecimal("6.75"), factura.getImpuestos());
        assertEquals(new BigDecimal("51.75"), factura.getTotal());
        // El servicio queda vinculado a la factura para que no se facture dos veces
        assertEquals(Long.valueOf(99L), consulta.getFacturaId());
        verify(historialRepository).save(consulta);
    }

    @Test
    void serviciosYaFacturadosLanzanBusinessException() {
        HistorialClinico consultaFacturada = HistorialClinico.builder()
                .id(11L)
                .precio(new BigDecimal("45.00"))
                .facturaId(55L)
                .build();
        when(historialRepository.findById(11L)).thenReturn(Optional.of(consultaFacturada));

        FacturaRequest request = new FacturaRequest(null, null, "Doble facturacion", null, null, null,
                null, null, null, null, List.of(11L), null, null, null, null, null, null);

        BusinessException excepcion = assertThrows(BusinessException.class,
                () -> facturacionService.crear(request));
        assertTrue(excepcion.getMessage().contains("ya fueron facturados"));
    }

    @Test
    void estadoDePagoInvalidoQuedaComoPendiente() {
        Factura guardada = facturacionService.crear(requestDirecto("Pago raro", new BigDecimal("10.00"), null));
        when(facturaRepository.findById(99L)).thenReturn(Optional.of(guardada));

        Factura actualizada = facturacionService.actualizarEstado(99L, Map.of("estadoPago", "HACKED"));

        assertEquals("Pendiente", actualizada.getEstadoPago());
    }

    @Test
    void elPrecioDeUnProductoSeResuelveEnElServidor() {
        Producto producto = Producto.builder()
                .id(5L)
                .codigo("MED-001")
                .nombre("Antipulgas")
                .categoria("Medicamentos")
                .precio(new BigDecimal("50.00"))
                .stock(10)
                .build();
        when(productoRepository.findByCodigo("MED-001")).thenReturn(Optional.of(producto));

        // El cliente intenta comprar a 1.00 USD un producto de 50.00 USD.
        FacturaRequest request = new FacturaRequest(null, null, "Venta de farmacia", null, null, null,
                null, null, null, null, null, null, null, null,
                List.of(new FacturaRequest.ItemFactura("Antipulgas", 2, new BigDecimal("1.00"), "MED-001", null)),
                null, null);

        Factura factura = facturacionService.crear(request);

        assertEquals(new BigDecimal("100.00"), factura.getSubtotal());
        assertEquals(new BigDecimal("15.00"), factura.getImpuestos());
        assertEquals(new BigDecimal("115.00"), factura.getTotal());
        // El stock se descuenta en la misma transaccion que la factura.
        verify(productoService).descontarStockPorVenta(eq("MED-001"), eq(2), anyString());
    }

    @Test
    void cuponPorcentualSeAplicaSobreElSubtotal() {
        Cupon cupon = Cupon.builder().id(3L).codigo("PROMO10").tipoDescuento(Cupon.PORCENTAJE)
                .valor(new BigDecimal("10.00")).build();
        when(cuponService.validarYCalcular(eq("PROMO10"), any()))
                .thenReturn(new CuponService.AplicacionCupon(cupon, "PROMO10", new BigDecimal("10.00")));

        FacturaRequest request = new FacturaRequest(null, null, "Venta con cupon", new BigDecimal("100.00"),
                null, null, null, null, null, null, null, null, null, null, null, "PROMO10", null);

        Factura factura = facturacionService.crear(request);

        assertEquals(new BigDecimal("90.00"), factura.getSubtotal());
        assertEquals(new BigDecimal("13.50"), factura.getImpuestos());
        assertEquals(new BigDecimal("103.50"), factura.getTotal());
        assertEquals(new BigDecimal("10.00"), factura.getDescuento());
        assertEquals("PROMO10", factura.getCuponCodigo());
        verify(cuponService).registrarCanje(eq(cupon), any(Factura.class), eq(new BigDecimal("10.00")));
    }

    @Test
    void cuponDeValorFijoSeDescuentaDelSubtotal() {
        Cupon cupon = Cupon.builder().id(4L).codigo("PROMO50").tipoDescuento(Cupon.VALOR_FIJO)
                .valor(new BigDecimal("50.00")).build();
        when(cuponService.validarYCalcular(eq("PROMO50"), any()))
                .thenReturn(new CuponService.AplicacionCupon(cupon, "PROMO50", new BigDecimal("50.00")));

        FacturaRequest request = new FacturaRequest(null, null, "Venta con cupon fijo", new BigDecimal("80.00"),
                null, null, null, null, null, null, null, null, null, null, null, "PROMO50", null);

        Factura factura = facturacionService.crear(request);

        assertEquals(new BigDecimal("30.00"), factura.getSubtotal());
        assertEquals(new BigDecimal("4.50"), factura.getImpuestos());
        assertEquals(new BigDecimal("34.50"), factura.getTotal());
        assertEquals(new BigDecimal("50.00"), factura.getDescuento());
    }

    @Test
    void cuponDeValorFijoQueCubreTodoElSubtotalSeRechaza() {
        Cupon cupon = Cupon.builder().id(5L).codigo("PROMO100").tipoDescuento(Cupon.VALOR_FIJO)
                .valor(new BigDecimal("100.00")).build();
        when(cuponService.validarYCalcular(eq("PROMO100"), any()))
                .thenReturn(new CuponService.AplicacionCupon(cupon, "PROMO100", new BigDecimal("20.00")));

        FacturaRequest request = new FacturaRequest(null, null, "Venta con cupon total", new BigDecimal("20.00"),
                null, null, null, null, null, null, null, null, null, null, null, "PROMO100", null);

        // El descuento se limita al subtotal: la factura no puede quedar en cero.
        BusinessException error = assertThrows(BusinessException.class, () -> facturacionService.crear(request));
        assertTrue(error.getMessage().toLowerCase().contains("subtotal"));
        verify(facturaRepository, never()).save(any(Factura.class));
    }

    @Test
    void unCuponNoAplicableRevierteLaFactura() {
        when(cuponService.validarYCalcular(eq("VENCIDO"), any()))
                .thenThrow(new BusinessException("El cupon VENCIDO ya expiro."));

        FacturaRequest request = new FacturaRequest(null, null, "Venta con cupon vencido", new BigDecimal("100.00"),
                null, null, null, null, null, null, null, null, null, null, null, "VENCIDO", null);

        assertThrows(BusinessException.class, () -> facturacionService.crear(request));
        verify(facturaRepository, never()).save(any(Factura.class));
    }

    @Test
    void stockInsuficienteImpideEmitirLaFactura() {
        Producto producto = Producto.builder()
                .id(6L)
                .codigo("MED-002")
                .nombre("Antiparasitario")
                .precio(new BigDecimal("20.00"))
                .stock(1)
                .build();
        when(productoRepository.findByCodigo("MED-002")).thenReturn(Optional.of(producto));

        FacturaRequest request = new FacturaRequest(null, null, "Venta sin stock", null, null, null,
                null, null, null, null, null, null, null, null,
                List.of(new FacturaRequest.ItemFactura("Antiparasitario", 5, new BigDecimal("20.00"), "MED-002", null)),
                null, null);

        BusinessException excepcion = assertThrows(BusinessException.class,
                () -> facturacionService.crear(request));
        assertTrue(excepcion.getMessage().contains("Stock insuficiente"));
    }
}


