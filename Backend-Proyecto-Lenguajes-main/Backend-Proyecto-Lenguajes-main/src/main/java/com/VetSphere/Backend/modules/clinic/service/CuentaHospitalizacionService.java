package com.VetSphere.Backend.modules.clinic.service;

import com.VetSphere.Backend.exception.BusinessException;
import com.VetSphere.Backend.exception.ResourceNotFoundException;
import com.VetSphere.Backend.modules.clinic.domain.*;
import com.VetSphere.Backend.modules.clinic.dto.FacturaRequest;
import com.VetSphere.Backend.modules.clinic.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;

/** Acumula servicios clinicos y farmacia hasta que se registra el alta. */
@Service
@RequiredArgsConstructor
public class CuentaHospitalizacionService {
    private final HospitalizacionRepository hospitalizacionRepository;
    private final HistorialClinicoRepository historialRepository;
    private final CirugiaRepository cirugiaRepository;
    private final OrdenLaboratorioRepository laboratorioRepository;
    private final ConsumoHospitalizacionRepository consumoRepository;
    private final ProductoService productoService;
    private final TarifaService tarifaService;
    private final FacturacionService facturacionService;

    @Value("${app.pagos.pichincha.numero-cuenta:}") private String numeroCuenta;
    @Value("${app.pagos.pichincha.titular:}") private String titular;
    @Value("${app.pagos.pichincha.identificacion:}") private String identificacion;
    @Value("${app.pagos.pichincha.qr-url:}") private String qrUrl;

    @Transactional(readOnly = true)
    public Map<String, Object> obtener(Long id) {
        Hospitalizacion h = hospitalizacion(id);
        return cuenta(h, LocalDateTime.now());
    }

    @Transactional
    public Map<String, Object> agregarConsumo(Long id, Map<String, Object> datos) {
        Hospitalizacion h = hospitalizacion(id);
        validarAbierta(h);
        String codigo = Objects.toString(datos.get("productoCodigo"), "").trim();
        int cantidad;
        try { cantidad = Integer.parseInt(Objects.toString(datos.get("cantidad"), "")); }
        catch (NumberFormatException ex) { throw new BusinessException("Ingresa una cantidad valida."); }
        if (codigo.isBlank() || cantidad < 1) throw new BusinessException("Selecciona un producto y una cantidad valida.");
        Producto producto = productoService.consultar(codigo);
        if (!producto.disponible()) throw new BusinessException("El producto ya no esta disponible.");
        BigDecimal precio = producto.getPrecio() == null ? BigDecimal.ZERO : producto.getPrecio();
        BigDecimal descuento = producto.descuentoEfectivo().divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP);
        BigDecimal precioNeto = precio.multiply(BigDecimal.ONE.subtract(descuento)).setScale(2, RoundingMode.HALF_UP);
        if (precioNeto.signum() <= 0) throw new BusinessException("El producto no tiene un precio facturable.");

        productoService.descontarStockPorConsumo(codigo, cantidad,
                "Consumo de farmacia en hospitalizacion " + id + " para " + h.getMascota().getNombre());
        consumoRepository.save(ConsumoHospitalizacion.builder().hospitalizacion(h).productoCodigo(producto.getCodigo())
                .productoNombre(producto.getNombre()).cantidad(cantidad).precioUnitario(precioNeto)
                .fecha(LocalDateTime.now()).build());
        return cuenta(h, LocalDateTime.now());
    }

    @Transactional
    public Factura darAlta(Long id) {
        Hospitalizacion h = hospitalizacion(id);
        validarAbierta(h);
        LocalDateTime fechaAlta = LocalDateTime.now();
        LocalDateTime ingreso = h.getIngreso() == null ? fechaAlta : h.getIngreso();
        List<Long> consultaIds = historialRepository.findByMascotaIdOrderByFechaDesc(h.getMascota().getId()).stream()
                .filter(x -> x.getFacturaId() == null && x.getFecha() != null && !x.getFecha().isBefore(ingreso))
                .map(HistorialClinico::getId).toList();
        List<Long> cirugiaIds = cirugiaRepository.findByMascotaId(h.getMascota().getId()).stream()
                .filter(x -> x.getFacturaId() == null && x.getFechaProgramada() != null && !x.getFechaProgramada().isBefore(ingreso)
                        && (x.getEstado() == null || !x.getEstado().toLowerCase(Locale.ROOT).contains("cancel")))
                .map(Cirugia::getId).toList();
        List<Long> laboratorioIds = laboratorioRepository.findByMascotaId(h.getMascota().getId()).stream()
                .filter(x -> x.getFacturaId() == null && x.getFecha() != null && !x.getFecha().isBefore(ingreso))
                .map(OrdenLaboratorio::getId).toList();
        List<FacturaRequest.ItemFactura> consumos = consumoRepository.findByHospitalizacionIdOrderByFechaAsc(id).stream()
                .map(x -> new FacturaRequest.ItemFactura(x.getProductoNombre(), x.getCantidad(), x.getPrecioUnitario(), null, "Farmacia"))
                .toList();

        if (consultaIds.isEmpty() && cirugiaIds.isEmpty() && laboratorioIds.isEmpty() && consumos.isEmpty()
                && (h.getPrecio() == null || h.getPrecio().signum() <= 0)) {
            throw new BusinessException("La cuenta no tiene cargos para facturar.");
        }
        FacturaRequest request = new FacturaRequest(null, fechaAlta,
                "Atencion y estancia clinica - " + h.getMascota().getNombre(), null, null, null,
                "Pendiente", null,
                h.getMascota().getPropietario() == null ? null : new FacturaRequest.IdRequest(h.getMascota().getPropietario().getId()),
                new FacturaRequest.IdRequest(h.getMascota().getId()), consultaIds, cirugiaIds, laboratorioIds,
                List.of(id), consumos, null, "Cuenta acumulada desde el ingreso hasta el alta.");
        Factura factura = facturacionService.crear(request);
        h.setAlta(fechaAlta);
        h.setEstado("Alta");
        hospitalizacionRepository.save(h);
        return factura;
    }

    public Map<String, Object> datosPago() {
        Map<String, Object> pago = new LinkedHashMap<>();
        pago.put("banco", "Banco Pichincha");
        pago.put("tipoCuenta", "Cuenta de ahorros");
        pago.put("numeroCuenta", numeroCuenta);
        pago.put("titular", titular);
        pago.put("identificacion", identificacion);
        pago.put("qrUrl", qrUrl);
        pago.put("configurado", !numeroCuenta.isBlank() && !titular.isBlank() && !identificacion.isBlank());
        return pago;
    }

    private Map<String, Object> cuenta(Hospitalizacion h, LocalDateTime hasta) {
        List<Map<String, Object>> items = new ArrayList<>();
        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal precio = h.getPrecio() == null ? tarifaService.precioHospitalizacion() : h.getPrecio();
        if (precio.signum() > 0) {
            items.add(item("Hospitalizacion - " + Optional.ofNullable(h.getMotivo()).orElse("estancia clinica"), 1, precio, "Estancia"));
            subtotal = subtotal.add(precio);
        }
        Long mascotaId = h.getMascota().getId();
        LocalDateTime ingreso = h.getIngreso() == null ? hasta : h.getIngreso();
        for (HistorialClinico x : historialRepository.findByMascotaIdOrderByFechaDesc(mascotaId)) {
            if (x.getFacturaId() != null || x.getFecha() == null || x.getFecha().isBefore(ingreso) || x.getFecha().isAfter(hasta)) continue;
            BigDecimal p = x.getPrecio() == null ? tarifaService.precioPorTipo(x.getTipoEvento()) : x.getPrecio();
            if (p.signum() <= 0) continue;
            items.add(item("Consulta - " + Optional.ofNullable(x.getTipoEvento()).orElse("atencion clinica") + ": " + Optional.ofNullable(x.getMotivo()).orElse(""), 1, p, "Consulta"));
            subtotal = subtotal.add(p);
        }
        for (Cirugia x : cirugiaRepository.findByMascotaId(mascotaId)) {
            if (x.getFacturaId() != null || x.getFechaProgramada() == null || x.getFechaProgramada().isBefore(ingreso) || x.getFechaProgramada().isAfter(hasta)) continue;
            BigDecimal p = x.getPrecio() == null ? tarifaService.precioCirugia() : x.getPrecio();
            if (p.signum() <= 0) continue;
            items.add(item("Cirugia - " + Optional.ofNullable(x.getProcedimiento()).orElse("procedimiento"), 1, p, "Cirugia"));
            subtotal = subtotal.add(p);
        }
        for (OrdenLaboratorio x : laboratorioRepository.findByMascotaId(mascotaId)) {
            if (x.getFacturaId() != null || x.getFecha() == null || x.getFecha().isBefore(ingreso) || x.getFecha().isAfter(hasta)) continue;
            BigDecimal p = x.getPrecio() == null ? tarifaService.precioLaboratorio() : x.getPrecio();
            if (p.signum() <= 0) continue;
            items.add(item("Laboratorio - " + Optional.ofNullable(x.getTipoPrueba()).orElse("prueba"), 1, p, "Laboratorio"));
            subtotal = subtotal.add(p);
        }
        for (ConsumoHospitalizacion x : consumoRepository.findByHospitalizacionIdOrderByFechaAsc(h.getId())) {
            Map<String, Object> i = item(x.getProductoNombre(), x.getCantidad(), x.getPrecioUnitario(), "Farmacia");
            i.put("fecha", x.getFecha()); items.add(i);
            subtotal = subtotal.add(x.getPrecioUnitario().multiply(BigDecimal.valueOf(x.getCantidad())));
        }
        subtotal = subtotal.setScale(2, RoundingMode.HALF_UP);
        BigDecimal iva = subtotal.multiply(new BigDecimal("0.15")).setScale(2, RoundingMode.HALF_UP);
        Map<String, Object> cuenta = new LinkedHashMap<>();
        cuenta.put("hospitalizacionId", h.getId());
        cuenta.put("mascota", h.getMascota().getNombre());
        cuenta.put("propietario", h.getMascota().getPropietario() == null ? "" : h.getMascota().getPropietario().getNombres() + " " + h.getMascota().getPropietario().getApellidos());
        cuenta.put("ingreso", h.getIngreso()); cuenta.put("items", items);
        cuenta.put("subtotal", subtotal); cuenta.put("iva", iva); cuenta.put("total", subtotal.add(iva));
        cuenta.put("facturaId", h.getFacturaId()); cuenta.put("cerrada", h.getFacturaId() != null || "Alta".equalsIgnoreCase(h.getEstado()));
        return cuenta;
    }

    private Map<String, Object> item(String descripcion, int cantidad, BigDecimal precio, String tipo) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("descripcion", descripcion); item.put("cantidad", cantidad); item.put("precioUnitario", precio);
        item.put("subtotal", precio.multiply(BigDecimal.valueOf(cantidad))); item.put("tipo", tipo);
        return item;
    }

    private Hospitalizacion hospitalizacion(Long id) {
        return hospitalizacionRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Hospitalizacion no encontrada."));
    }

    private void validarAbierta(Hospitalizacion h) {
        if (h.getFacturaId() != null || h.getAlta() != null || "Alta".equalsIgnoreCase(h.getEstado())) {
            throw new BusinessException("La hospitalizacion ya esta cerrada o facturada.");
        }
        if (h.getMascota() == null || h.getMascota().getId() == null) throw new BusinessException("La hospitalizacion no tiene mascota asignada.");
    }
}
