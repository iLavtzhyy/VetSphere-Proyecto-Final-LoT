package com.VetSphere.Backend.modules.clinic.service;

import com.VetSphere.Backend.exception.BusinessException;
import com.VetSphere.Backend.exception.ResourceNotFoundException;
import com.VetSphere.Backend.modules.clinic.domain.*;
import com.VetSphere.Backend.modules.clinic.dto.FacturaRequest;
import com.VetSphere.Backend.modules.clinic.dto.LineaFacturable;
import com.VetSphere.Backend.modules.clinic.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Logica de facturacion movida fuera del controlador.
 *
 * Punto critico de seguridad: el backend NUNCA confia en los totales enviados
 * por el cliente. El subtotal se recalcula desde los servicios clinicos
 * vinculados o desde los items, y para los items con {@code productoCodigo} se
 * usa SIEMPRE el precio del catalogo. Los impuestos se derivan del subtotal con
 * IVA 15% y los cupones se validan y calculan en el servidor.
 */
@Service
@RequiredArgsConstructor
public class FacturacionService {
    public static final BigDecimal IVA = new BigDecimal("0.15");
    private static final BigDecimal SUBTOTAL_MAXIMO = new BigDecimal("9999999.99");
    private static final BigDecimal CIEN = new BigDecimal("100");
    private static final List<String> ESTADOS_PAGO = List.of("Pagado", "Pendiente", "Anulado");

    private final FacturaRepository facturaRepository;
    private final FacturaDetalleRepository detalleRepository;
    private final HistorialClinicoRepository historialRepository;
    private final CirugiaRepository cirugiaRepository;
    private final OrdenLaboratorioRepository ordenRepository;
    private final HospitalizacionRepository hospitalizacionRepository;
    private final UsuarioRepository usuarioRepository;
    private final MascotaRepository mascotaRepository;
    private final ProductoRepository productoRepository;
    private final ProductoService productoService;
    private final CuponService cuponService;
    private final TarifaService tarifaService;
    private final AuditoriaService auditoriaService;

    /** Resultado del calculo completo de una factura. */
    private record Calculo(
            List<LineaFacturable> lineas,
            BigDecimal bruto,
            BigDecimal descuentoLineas,
            BigDecimal descuentoCupon,
            BigDecimal subtotal,
            BigDecimal impuestos,
            BigDecimal total,
            Cupon cupon,
            String cuponCodigo
    ) {
    }

    public List<Map<String, Object>> listar() {
        return facturaRepository.findAll().stream().map(this::mapear).toList();
    }

    public List<Map<String, Object>> mapear(List<Factura> facturas) {
        return facturas.stream().map(this::mapear).toList();
    }

    public List<Map<String, Object>> listar(int pagina, int tamano) {
        var page = facturaRepository.findAll(org.springframework.data.domain.PageRequest.of(pagina, tamano));
        return page.getContent().stream().map(this::mapear).toList();
    }

    public long total() {
        return facturaRepository.count();
    }

    @Transactional
    public Factura crear(FacturaRequest request) {
        Calculo calculo = calcular(request);

        Factura factura = Factura.builder()
                .numero(numero(request.numero()))
                .fecha(request.fecha() == null ? LocalDateTime.now() : request.fecha())
                .concepto(request.concepto())
                .subtotal(calculo.subtotal())
                .impuestos(calculo.impuestos())
                .total(calculo.total())
                .descuento(calculo.descuentoLineas().add(calculo.descuentoCupon())
                        .setScale(2, RoundingMode.HALF_UP))
                .cuponCodigo(calculo.cuponCodigo())
                .observaciones(request.observaciones())
                .estadoPago(estadoPago(request.estadoPago()))
                .metodoPago(request.metodoPago())
                .cliente(request.cliente() != null && request.cliente().id() != null
                        ? usuarioRepository.findById(request.cliente().id()).orElse(null) : null)
                .mascota(request.mascota() != null && request.mascota().id() != null
                        ? mascotaRepository.findById(request.mascota().id()).orElse(null) : null)
                .build();

        Factura guardada = facturaRepository.save(factura);
        persistirDetalle(guardada, calculo.lineas());
        descontarStock(calculo.lineas(), guardada.getNumero());
        if (calculo.cupon() != null && calculo.descuentoCupon().signum() > 0) {
            cuponService.registrarCanje(calculo.cupon(), guardada, calculo.descuentoCupon());
        }
        vincularServicios(guardada.getId(), request);

        auditoriaService.registrar(AuditoriaService.FACTURA_EMITIDA, "Factura", guardada.getId(),
                "Factura " + guardada.getNumero() + " por " + guardada.getTotal() + " (subtotal "
                        + guardada.getSubtotal() + " + IVA " + guardada.getImpuestos() + ")"
                        + (calculo.cuponCodigo() != null ? " cupon " + calculo.cuponCodigo() : ""));
        return guardada;
    }

    /** Devuelve una vista serializable mientras la sesion de persistencia sigue abierta. */
    @Transactional
    public Map<String, Object> crearMapeada(FacturaRequest request) {
        return mapear(crear(request));
    }

    /**
     * Simula el resultado de aplicar un cupon sobre un carrito, sin persistir
     * nada. Usa exactamente el mismo calculo que {@link #crear(FacturaRequest)},
     * por lo que el importe mostrado en pantalla coincide con el que se cobra.
     */
    @Transactional(readOnly = true)
    public Map<String, Object> previsualizarCupon(FacturaRequest request) {
        Calculo calculo = calcular(request);
        Map<String, Object> resultado = new LinkedHashMap<>();
        resultado.put("codigo", calculo.cuponCodigo());
        resultado.put("aplicado", calculo.descuentoCupon().signum() > 0);
        resultado.put("bruto", calculo.bruto());
        resultado.put("descuento", calculo.descuentoCupon());
        resultado.put("subtotal", calculo.subtotal());
        resultado.put("impuestos", calculo.impuestos());
        resultado.put("total", calculo.total());
        return resultado;
    }

    @Transactional
    public Factura actualizarEstado(Long id, Map<String, String> request) {
        Factura factura = facturaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Factura no encontrada"));
        String estado = request.getOrDefault("estadoPago", "Pendiente");
        if (!ESTADOS_PAGO.contains(estado)) {
            estado = "Pendiente";
        }
        factura.setEstadoPago(estado);
        if (request.containsKey("metodoPago")) {
            factura.setMetodoPago(request.get("metodoPago"));
        }
        return facturaRepository.save(factura);
    }

    /** Evita exponer relaciones lazy de JPA en la respuesta HTTP. */
    @Transactional
    public Map<String, Object> actualizarEstadoMapeado(Long id, Map<String, String> request) {
        return mapear(actualizarEstado(id, request));
    }

    // ------------------------------------------------------------------
    // Calculo
    // ------------------------------------------------------------------

    /**
     * Recalcula la factura completa en el servidor. Orden de prioridad:
     * 1) servicios clinicos seleccionados (precios reales de la base de datos);
     * 2) items explicitos (el precio de los productos se toma del catalogo);
     * 3) subtotal "directo" del payload (facturacion directa), saneado.
     * Los impuestos, el descuento y el total NUNCA se toman del cliente.
     */
    private Calculo calcular(FacturaRequest request) {
        List<LineaFacturable> lineas = new ArrayList<>();

        if (hayServiciosSeleccionados(request)) {
            lineas.addAll(lineasDeServicios(request));
            // Una cuenta acumulada puede incluir consumos de farmacia ademas de
            // servicios clinicos. Los items con precio libre se agregan al detalle;
            // el caller interno ya desconto el stock al registrar el consumo.
            if (request.items() != null && !request.items().isEmpty()) {
                lineas.addAll(lineasDeItems(request));
            }
        } else if (request.items() != null && !request.items().isEmpty()) {
            lineas.addAll(lineasDeItems(request));
        } else {
            lineas.add(lineaDirecta(request));
        }

        BigDecimal bruto = BigDecimal.ZERO;
        BigDecimal descuentoLineas = BigDecimal.ZERO;
        for (LineaFacturable linea : lineas) {
            bruto = bruto.add(linea.precioUnitario().multiply(BigDecimal.valueOf(linea.cantidad())));
            descuentoLineas = descuentoLineas.add(linea.descuento());
        }
        bruto = bruto.setScale(2, RoundingMode.HALF_UP);
        descuentoLineas = descuentoLineas.setScale(2, RoundingMode.HALF_UP);

        BigDecimal base = bruto.subtract(descuentoLineas);
        if (base.signum() < 0) {
            throw new BusinessException("Los descuentos aplicados superan el importe de la factura.");
        }

        CuponService.AplicacionCupon aplicacion = cuponService.validarYCalcular(request.cuponCodigo(), lineas);
        BigDecimal descuentoCupon = aplicacion.aplicado()
                ? aplicacion.descuento().min(base)
                : BigDecimal.ZERO;

        BigDecimal subtotal = base.subtract(descuentoCupon).setScale(2, RoundingMode.HALF_UP);
        if (subtotal.signum() <= 0) {
            throw new BusinessException("El subtotal de la factura debe ser mayor a 0.");
        }
        if (subtotal.compareTo(SUBTOTAL_MAXIMO) > 0) {
            throw new BusinessException("El subtotal de la factura supera el maximo permitido.");
        }
        BigDecimal impuestos = subtotal.multiply(IVA).setScale(2, RoundingMode.HALF_UP);
        BigDecimal total = subtotal.add(impuestos);

        return new Calculo(lineas, bruto, descuentoLineas, descuentoCupon, subtotal, impuestos, total,
                aplicacion.cupon(), aplicacion.codigo());
    }

    /** Lineas generadas por los servicios clinicos seleccionados. */
    private List<LineaFacturable> lineasDeServicios(FacturaRequest request) {
        List<LineaFacturable> lineas = new ArrayList<>();
        BigDecimal suma = BigDecimal.ZERO;

        for (Long id : lista(request.consultaIds())) {
            HistorialClinico historial = historialRepository.findById(id).orElseThrow(
                    () -> new BusinessException("La consulta " + id + " no existe."));
            if (historial.getFacturaId() != null) continue;
            validarPertenencia(historial.getMascota(), request, "La consulta " + id);
            BigDecimal precio = historial.getPrecio() != null ? historial.getPrecio()
                    : tarifaService.precioPorTipo(historial.getTipoEvento());
            suma = suma.add(precio);
            lineas.add(linea("Consulta " + (historial.getTipoEvento() == null ? "clinica" : historial.getTipoEvento()),
                    "Servicio", 1, precio, BigDecimal.ZERO));
        }
        for (Long id : lista(request.cirugiaIds())) {
            Cirugia cirugia = cirugiaRepository.findById(id).orElseThrow(
                    () -> new BusinessException("La cirugia " + id + " no existe."));
            if (cirugia.getFacturaId() != null) continue;
            validarPertenencia(cirugia.getMascota(), request, "La cirugia " + id);
            BigDecimal precio = cirugia.getPrecio() != null ? cirugia.getPrecio() : tarifaService.precioCirugia();
            suma = suma.add(precio);
            lineas.add(linea("Cirugia", "Servicio", 1, precio, BigDecimal.ZERO));
        }
        for (Long id : lista(request.laboratorioIds())) {
            OrdenLaboratorio orden = ordenRepository.findById(id).orElseThrow(
                    () -> new BusinessException("La orden de laboratorio " + id + " no existe."));
            if (orden.getFacturaId() != null) continue;
            validarPertenencia(orden.getMascota(), request, "La orden de laboratorio " + id);
            BigDecimal precio = orden.getPrecio() != null ? orden.getPrecio() : tarifaService.precioLaboratorio();
            suma = suma.add(precio);
            lineas.add(linea("Laboratorio", "Servicio", 1, precio, BigDecimal.ZERO));
        }
        for (Long id : lista(request.hospitalizacionIds())) {
            Hospitalizacion hospitalizacion = hospitalizacionRepository.findById(id).orElseThrow(
                    () -> new BusinessException("La hospitalizacion " + id + " no existe."));
            if (hospitalizacion.getFacturaId() != null) continue;
            validarPertenencia(hospitalizacion.getMascota(), request, "La hospitalizacion " + id);
            BigDecimal precio = hospitalizacion.getPrecio() != null ? hospitalizacion.getPrecio()
                    : tarifaService.precioHospitalizacion();
            suma = suma.add(precio);
            lineas.add(linea("Hospitalizacion", "Servicio", 1, precio, BigDecimal.ZERO));
        }
        if (suma.signum() <= 0) {
            throw new BusinessException("Los servicios seleccionados ya fueron facturados o no tienen precio.");
        }
        return lineas;
    }

    /**
     * Lineas generadas por los items del payload. Si el item referencia un
     * producto del catalogo, el precio y el descuento se toman de la base de
     * datos: el cliente no puede manipular el importe a cobrar.
     */
    private List<LineaFacturable> lineasDeItems(FacturaRequest request) {
        List<LineaFacturable> lineas = new ArrayList<>();
        for (FacturaRequest.ItemFactura item : request.items()) {
            int cantidad = item.cantidad() == null ? 1 : item.cantidad();
            if (cantidad <= 0) {
                throw new BusinessException("Item de factura invalido: cantidad no valida.");
            }
            if (item.productoCodigo() != null && !item.productoCodigo().isBlank()) {
                Producto producto = productoRepository.findByCodigo(item.productoCodigo().trim())
                        .orElseThrow(() -> new BusinessException("Producto no encontrado: " + item.productoCodigo()));
                if (!producto.disponible()) {
                    throw new BusinessException("El producto " + producto.getCodigo() + " esta retirado de la operacion.");
                }
                BigDecimal precio = producto.getPrecio() == null ? BigDecimal.ZERO : producto.getPrecio();
                BigDecimal descuentoUnitario = precio.multiply(producto.descuentoEfectivo())
                        .divide(CIEN, 2, RoundingMode.HALF_UP);
                BigDecimal subtotal = precio.multiply(BigDecimal.valueOf(cantidad))
                        .subtract(descuentoUnitario.multiply(BigDecimal.valueOf(cantidad)))
                        .setScale(2, RoundingMode.HALF_UP);
                validarStock(producto, cantidad);
                lineas.add(new LineaFacturable(producto.getCodigo(), producto.getNombre(),
                        producto.getCategoria(), cantidad, precio.setScale(2, RoundingMode.HALF_UP),
                        descuentoUnitario.multiply(BigDecimal.valueOf(cantidad)).setScale(2, RoundingMode.HALF_UP),
                        subtotal));
            } else {
                if (item.precioUnitario() == null || item.precioUnitario().signum() < 0) {
                    throw new BusinessException("Item de factura invalido: precio unitario no valido.");
                }
                BigDecimal precio = item.precioUnitario().setScale(2, RoundingMode.HALF_UP);
                String descripcion = item.descripcion() == null || item.descripcion().isBlank()
                        ? "Concepto sin descripcion" : item.descripcion();
                lineas.add(new LineaFacturable(null, descripcion, item.categoria(), cantidad, precio,
                        BigDecimal.ZERO, precio.multiply(BigDecimal.valueOf(cantidad)).setScale(2, RoundingMode.HALF_UP)));
            }
        }
        if (lineas.isEmpty()) {
            throw new BusinessException("La factura no contiene lineas validas.");
        }
        return lineas;
    }

    /** Facturacion directa: un unico concepto con el subtotal saneado. */
    private LineaFacturable lineaDirecta(FacturaRequest request) {
        BigDecimal directo = request.subtotal();
        if (directo == null || directo.signum() <= 0) {
            throw new BusinessException("El subtotal de la factura debe ser mayor a 0.");
        }
        if (directo.compareTo(SUBTOTAL_MAXIMO) > 0) {
            throw new BusinessException("El subtotal de la factura supera el maximo permitido.");
        }
        BigDecimal importe = directo.setScale(2, RoundingMode.HALF_UP);
        return new LineaFacturable(null, request.concepto(), "Concepto", 1, importe, BigDecimal.ZERO, importe);
    }

    private LineaFacturable linea(String descripcion, String categoria, int cantidad,
                                  BigDecimal precio, BigDecimal descuento) {
        BigDecimal p = precio.setScale(2, RoundingMode.HALF_UP);
        BigDecimal d = descuento.setScale(2, RoundingMode.HALF_UP);
        return new LineaFacturable(null, descripcion, categoria, cantidad, p, d,
                p.multiply(BigDecimal.valueOf(cantidad)).subtract(d).setScale(2, RoundingMode.HALF_UP));
    }

    /** Comprueba que hay existencias suficientes antes de emitir la factura. */
    private void validarStock(Producto producto, int cantidad) {
        int stock = producto.getStock() == null ? 0 : producto.getStock();
        if (stock < cantidad) {
            throw new BusinessException("Stock insuficiente de " + producto.getCodigo()
                    + ": disponibles " + stock + ", solicitados " + cantidad + ".");
        }
    }

    /** Persiste las lineas de la factura (clave para medir la rotacion). */
    private void persistirDetalle(Factura factura, List<LineaFacturable> lineas) {
        for (LineaFacturable linea : lineas) {
            Producto producto = linea.productoCodigo() == null ? null
                    : productoRepository.findByCodigo(linea.productoCodigo()).orElse(null);
            detalleRepository.save(FacturaDetalle.builder()
                    .factura(factura)
                    .producto(producto)
                    .descripcion(linea.descripcion())
                    .cantidad(linea.cantidad())
                    .precioUnitario(linea.precioUnitario())
                    .descuento(linea.descuento())
                    .subtotal(linea.subtotal())
                    .build());
        }
    }

    /** Descuenta el stock de los productos vendidos (transaccional con la factura). */
    private void descontarStock(List<LineaFacturable> lineas, String numeroFactura) {
        for (LineaFacturable linea : lineas) {
            if (linea.productoCodigo() == null) {
                continue;
            }
            productoService.descontarStockPorVenta(linea.productoCodigo(), linea.cantidad(),
                    "Venta en la factura " + numeroFactura);
        }
    }

    private boolean hayServiciosSeleccionados(FacturaRequest request) {
        return (request.consultaIds() != null && !request.consultaIds().isEmpty())
                || (request.cirugiaIds() != null && !request.cirugiaIds().isEmpty())
                || (request.laboratorioIds() != null && !request.laboratorioIds().isEmpty())
                || (request.hospitalizacionIds() != null && !request.hospitalizacionIds().isEmpty());
    }

    /** El servicio clinico debe pertenecer a la mascota/cliente indicados en la factura. */
    private void validarPertenencia(Mascota mascotaServicio, FacturaRequest request, String contexto) {
        if (request.mascota() != null && request.mascota().id() != null
                && mascotaServicio != null && mascotaServicio.getId() != null
                && !mascotaServicio.getId().equals(request.mascota().id())) {
            throw new BusinessException(contexto + " no pertenece a la mascota indicada.");
        }
        if (request.cliente() != null && request.cliente().id() != null
                && mascotaServicio != null && mascotaServicio.getPropietario() != null
                && !mascotaServicio.getPropietario().getId().equals(request.cliente().id())) {
            throw new BusinessException(contexto + " no pertenece al cliente indicado.");
        }
    }

    private void vincularServicios(Long facturaId, FacturaRequest request) {
        for (Long id : lista(request.consultaIds())) {
            historialRepository.findById(id).ifPresent(h -> {
                h.setFacturaId(facturaId);
                historialRepository.save(h);
            });
        }
        for (Long id : lista(request.cirugiaIds())) {
            cirugiaRepository.findById(id).ifPresent(c -> {
                c.setFacturaId(facturaId);
                cirugiaRepository.save(c);
            });
        }
        for (Long id : lista(request.laboratorioIds())) {
            ordenRepository.findById(id).ifPresent(o -> {
                o.setFacturaId(facturaId);
                ordenRepository.save(o);
            });
        }
        for (Long id : lista(request.hospitalizacionIds())) {
            hospitalizacionRepository.findById(id).ifPresent(h -> {
                h.setFacturaId(facturaId);
                hospitalizacionRepository.save(h);
            });
        }
    }

    private List<Long> lista(List<Long> ids) {
        return ids == null ? List.of() : ids;
    }

    private String estadoPago(String estado) {
        return estado == null || !ESTADOS_PAGO.contains(estado) ? "Pendiente" : estado;
    }

    private String numero(String numero) {
        if (numero != null && !numero.isBlank()) return numero;
        long consecutivo = facturaRepository.count() + 1;
        return "FAC-" + LocalDateTime.now().getYear() + "-" + String.format("%06d", consecutivo);
    }

    private Map<String, Object> mapear(Factura f) {
        Map<String, Object> mapa = new LinkedHashMap<>();
        mapa.put("id", f.getId());
        mapa.put("numero", f.getNumero());
        mapa.put("fecha", f.getFecha() == null ? "" : f.getFecha().toString());
        mapa.put("cliente", f.getCliente() == null ? "Sin cliente" : f.getCliente().getNombres() + " " + f.getCliente().getApellidos());
        mapa.put("mascota", f.getMascota() == null ? "Sin mascota" : f.getMascota().getNombre());
        mapa.put("concepto", f.getConcepto());
        mapa.put("subtotal", f.getSubtotal());
        mapa.put("impuestos", f.getImpuestos());
        mapa.put("descuento", f.descuentoEfectivo());
        mapa.put("cuponCodigo", f.getCuponCodigo() == null ? "" : f.getCuponCodigo());
        mapa.put("total", f.getTotal());
        mapa.put("estadoPago", f.getEstadoPago());
        mapa.put("metodoPago", f.getMetodoPago() == null ? "" : f.getMetodoPago());
        mapa.put("observaciones", f.getObservaciones() == null ? "" : f.getObservaciones());
        return mapa;
    }
}

