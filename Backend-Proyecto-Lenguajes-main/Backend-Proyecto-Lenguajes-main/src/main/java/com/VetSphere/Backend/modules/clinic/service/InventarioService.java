package com.VetSphere.Backend.modules.clinic.service;

import com.VetSphere.Backend.modules.clinic.domain.Cupon;
import com.VetSphere.Backend.modules.clinic.domain.Producto;
import com.VetSphere.Backend.modules.clinic.dto.CuponAutoRequest;
import com.VetSphere.Backend.modules.clinic.repository.CuponRepository;
import com.VetSphere.Backend.modules.clinic.repository.FacturaDetalleRepository;
import com.VetSphere.Backend.modules.clinic.repository.ProductoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Analisis de rotacion de inventario de la farmacia.
 *
 * Responde la pregunta "que productos no se estan vendiendo" con datos reales:
 * las lineas de factura persistidas (FacturaDetalle) permiten saber cuantas
 * unidades salio de cada producto, cuando fue la ultima venta y cuanto capital
 * esta dormido en el almacen.
 *
 * Con ese analisis se generan automaticamente cupones de descuento para los
 * articulos de baja rotacion.
 */
@Service
@RequiredArgsConstructor
public class InventarioService {
    /** Producto que nunca registro una venta. */
    public static final String SIN_ROTACION = "SIN_ROTACION";
    /** Producto que lleva mas dias sin vender que el umbral configurado. */
    public static final String BAJA_ROTACION = "BAJA_ROTACION";
    /** Producto con ventas recientes. */
    public static final String ROTACION_SALUDABLE = "ROTACION_SALUDABLE";

    /** Ventana por defecto del analisis de ventas (dias). */
    public static final int DIAS_VENTANA_POR_DEFECTO = 30;
    /** Umbral por defecto de dias sin vender paraconsiderar baja rotacion. */
    public static final int DIAS_UMBRAL_POR_DEFECTO = 30;

    private final ProductoRepository productoRepository;
    private final FacturaDetalleRepository detalleRepository;
    private final CuponRepository cuponRepository;
    private final CuponService cuponService;
    private final AuditoriaService auditoriaService;

    /**
     * Analiza la rotacion del inventario.
     *
     * @param diasVentana  ventana de analisis de ventas (por defecto 30 dias)
     * @param diasUmbral   dias sin vender a partir de los cuales el producto
     *                     se considera de baja rotacion (por defecto 30 dias)
     */
    public Map<String, Object> analizarRotacion(Integer diasVentana, Integer diasUmbral) {
        int ventana = diasVentana == null || diasVentana <= 0 ? DIAS_VENTANA_POR_DEFECTO : diasVentana;
        int umbral = diasUmbral == null || diasUmbral <= 0 ? DIAS_UMBRAL_POR_DEFECTO : diasUmbral;
        LocalDateTime desde = LocalDateTime.now().minusDays(ventana);

        Map<Long, long[]> unidadesPorProducto = new HashMap<>();
        Map<Long, BigDecimal> ingresosPorProducto = new HashMap<>();
        Map<Long, LocalDateTime> ultimaVenta = new HashMap<>();
        Map<Long, Double> unidadesTotales = new HashMap<>();

        for (Object[] fila : detalleRepository.ventasPorProducto(desde)) {
            unidadesPorProducto.put((Long) fila[0], new long[]{numero(fila[1]).longValue()});
            ingresosPorProducto.put((Long) fila[0], valorMoneda(fila[2]));
        }
        for (Object[] fila : detalleRepository.ultimaVentaPorProducto()) {
            ultimaVenta.put((Long) fila[0], (LocalDateTime) fila[1]);
        }
        for (Object[] fila : detalleRepository.unidadesTotalesPorProducto()) {
            unidadesTotales.put((Long) fila[0], numero(fila[1]).doubleValue());
        }

        List<Map<String, Object>> productos = new ArrayList<>();
        BigDecimal capitalTotal = BigDecimal.ZERO;
        BigDecimal capitalDormido = BigDecimal.ZERO;
        int sinRotacion = 0;
        int bajaRotacion = 0;
        long unidadesVendidas = 0;
        BigDecimal ingresosVentana = BigDecimal.ZERO;

        for (Producto producto : productoRepository.findAll()) {
            if (producto.getId() == null) {
                continue;
            }
            long[] ventanaDatos = unidadesPorProducto.getOrDefault(producto.getId(), new long[]{0L});
            long unidades = ventanaDatos[0];
            LocalDateTime ultima = ultimaVenta.get(producto.getId());
            Long diasSinVenta = ultima == null ? null : Duration.between(ultima, LocalDateTime.now()).toDays();

            String estado = clasificar(ultima, diasSinVenta, umbral);
            int stock = producto.getStock() == null ? 0 : producto.getStock();
            BigDecimal precio = producto.getPrecio() == null ? BigDecimal.ZERO : producto.getPrecio();
            BigDecimal valorInventario = precio.multiply(BigDecimal.valueOf(stock)).setScale(2, RoundingMode.HALF_UP);

            BigDecimal ingresos = ingresosPorProducto.getOrDefault(producto.getId(), BigDecimal.ZERO)
                    .setScale(2, RoundingMode.HALF_UP);
            // Cobertura en dias: cuanto inventario queda con el ritmo de venta actual.
            Integer diasCobertura = null;
            if (unidades > 0) {
                double ritmoDiario = (double) unidades / ventana;
                diasCobertura = (int) Math.floor(stock / ritmoDiario);
            } else if (stock > 0) {
                diasCobertura = -1; // -1 = sin ventas en la ventana, cobertura indeterminada
            }

            Map<String, Object> fila = new LinkedHashMap<>();
            fila.put("id", producto.getId());
            fila.put("codigo", producto.getCodigo());
            fila.put("nombre", producto.getNombre());
            fila.put("categoria", producto.getCategoria());
            fila.put("stock", stock);
            fila.put("stockCritico", producto.getStockCritico());
            fila.put("precio", precio.setScale(2, RoundingMode.HALF_UP));
            fila.put("descuentoProducto", producto.descuentoEfectivo());
            fila.put("unidadesVendidas", unidades);
            fila.put("unidadesTotales", (long) unidadesTotales.getOrDefault(producto.getId(), 0d).doubleValue());
            fila.put("ingresos", ingresos);
            fila.put("ultimaVenta", ultima);
            fila.put("diasSinVenta", diasSinVenta);
            fila.put("diasCobertura", diasCobertura);
            fila.put("valorInventario", valorInventario);
            fila.put("estado", estado);
            fila.put("descuentoSugerido", descuentoSugerido(diasSinVenta));
            fila.put("cuponActivo", cuponActivoPara(producto.getId()));
            productos.add(fila);

            capitalTotal = capitalTotal.add(valorInventario);
            ingresosVentana = ingresosVentana.add(ingresos);
            unidadesVendidas += unidades;
            if (SIN_ROTACION.equals(estado)) {
                sinRotacion++;
                capitalDormido = capitalDormido.add(valorInventario);
            } else if (BAJA_ROTACION.equals(estado)) {
                bajaRotacion++;
                capitalDormido = capitalDormido.add(valorInventario);
            }
        }

        productos.sort(Comparator
                .comparing((Map<String, Object> p) -> estadoPrioridad(String.valueOf(p.get("estado"))))
                .thenComparing(p -> {
                    Object dias = p.get("diasSinVenta");
                    return dias == null ? Long.MAX_VALUE : (Long) dias;
                }, Comparator.reverseOrder())
                .thenComparing(p -> new BigDecimal(p.get("valorInventario").toString()),
                        Comparator.reverseOrder()));

        Map<String, Object> resumen = new LinkedHashMap<>();
        resumen.put("diasVentana", ventana);
        resumen.put("diasUmbral", umbral);
        resumen.put("totalProductos", productos.size());
        resumen.put("productosSinRotacion", sinRotacion);
        resumen.put("productosBajaRotacion", bajaRotacion);
        resumen.put("productosSaludables", productos.size() - sinRotacion - bajaRotacion);
        resumen.put("unidadesVendidas", unidadesVendidas);
        resumen.put("ingresos", ingresosVentana.setScale(2, RoundingMode.HALF_UP));
        resumen.put("capitalInventario", capitalTotal.setScale(2, RoundingMode.HALF_UP));
        resumen.put("capitalDormido", capitalDormido.setScale(2, RoundingMode.HALF_UP));

        Map<String, Object> resultado = new LinkedHashMap<>();
        resultado.put("resumen", resumen);
        resultado.put("productos", productos);
        return resultado;
    }

    /**
     * Genera automaticamente cupones de descuento para los productos de baja
     * rotacion que aun no tienen una promocion activa.
     */
    @Transactional
    public Map<String, Object> generarCuponesBajaRotacion(CuponAutoRequest request) {
        int umbral = request.diasSinVenta() == null || request.diasSinVenta() <= 0
                ? DIAS_UMBRAL_POR_DEFECTO : request.diasSinVenta();
        int duracion = request.duracionDias() == null || request.duracionDias() <= 0 ? 30 : request.duracionDias();
        BigDecimal descuentoForzado = request.descuentoPorcentaje();

        Map<String, Object> analisis = analizarRotacion(DIAS_VENTANA_POR_DEFECTO, umbral);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> productos = (List<Map<String, Object>>) analisis.get("productos");

        List<Cupon> creados = new ArrayList<>();
        List<Map<String, Object>> omitidos = new ArrayList<>();

        for (Map<String, Object> fila : productos) {
            String estado = String.valueOf(fila.get("estado"));
            if (ROTACION_SALUDABLE.equals(estado)) {
                continue;
            }
            Long productoId = (Long) fila.get("id");
            int stock = (int) (fila.get("stock") == null ? 0 : (Integer) fila.get("stock"));
            if (stock <= 0) {
                omitidos.add(Map.of("codigo", String.valueOf(fila.get("codigo")),
                        "motivo", "No tiene existencias en stock"));
                continue;
            }
            if (Boolean.TRUE.equals(fila.get("cuponActivo"))) {
                omitidos.add(Map.of("codigo", String.valueOf(fila.get("codigo")),
                        "motivo", "Ya tiene una promocion activa"));
                continue;
            }
            Producto producto = productoRepository.findById(productoId).orElse(null);
            if (producto == null) {
                continue;
            }
            BigDecimal descuento = descuentoForzado != null && descuentoForzado.signum() > 0
                    ? descuentoForzado
                    : (BigDecimal) fila.get("descuentoSugerido");
            Object diasSinVenta = fila.get("diasSinVenta");
            String descripcion = "Promocion de baja rotacion: " + producto.getNombre()
                    + (diasSinVenta == null
                    ? " (sin ventas registradas)"
                    : " (sin ventas hace " + diasSinVenta + " dias)");
            creados.add(cuponService.crearParaBajaRotacion(producto, descuento, duracion, descripcion));
        }

        auditoriaService.registrar(AuditoriaService.CUPON_CREADO, "Cupon", null,
                "Generacion automatica de promociones de baja rotacion: " + creados.size()
                        + " cupon(es) creados, " + omitidos.size() + " omitido(s)");

        Map<String, Object> resultado = new LinkedHashMap<>();
        resultado.put("creados", creados.size());
        resultado.put("omitidos", omitidos.size());
        resultado.put("cupones", creados);
        resultado.put("detalleOmitidos", omitidos);
        return resultado;
    }

    private String clasificar(LocalDateTime ultimaVenta, Long diasSinVenta, int umbral) {
        if (ultimaVenta == null) {
            return SIN_ROTACION;
        }
        if (diasSinVenta == null || diasSinVenta >= umbral) {
            return BAJA_ROTACION;
        }
        return ROTACION_SALUDABLE;
    }

    private int estadoPrioridad(String estado) {
        return switch (estado) {
            case SIN_ROTACION -> 0;
            case BAJA_ROTACION -> 1;
            default -> 2;
        };
    }

    /**
     * Descuento recomendado segun los dias que el producto lleva sin venderse.
     * El tope es 30% para no erosionar el margen de forma irreversible.
     */
    private BigDecimal descuentoSugerido(Long diasSinVenta) {
        if (diasSinVenta == null) {
            return new BigDecimal("30.00");
        }
        long dias = diasSinVenta;
        if (dias <= 30) return new BigDecimal("5.00");
        if (dias <= 60) return new BigDecimal("10.00");
        if (dias <= 90) return new BigDecimal("15.00");
        if (dias <= 180) return new BigDecimal("20.00");
        return new BigDecimal("25.00");
    }

    private boolean cuponActivoPara(Long productoId) {
        LocalDateTime ahora = LocalDateTime.now();
        return cuponRepository.findVigentes(ahora).stream()
                .anyMatch(c -> c.getProducto() != null && productoId.equals(c.getProducto().getId()));
    }

    private Number numero(Object valor) {
        if (valor == null) {
            return 0L;
        }
        if (valor instanceof Number n) {
            return n;
        }
        try {
            return new BigDecimal(valor.toString());
        } catch (NumberFormatException ex) {
            return 0L;
        }
    }

    /** Convierte un importe agregado de la base de datos a BigDecimal con 2 decimales. */
    private BigDecimal valorMoneda(Object valor) {
        if (valor == null) {
            return BigDecimal.ZERO;
        }
        try {
            return new BigDecimal(valor.toString()).setScale(2, RoundingMode.HALF_UP);
        } catch (NumberFormatException ex) {
            return BigDecimal.ZERO;
        }
    }
}
