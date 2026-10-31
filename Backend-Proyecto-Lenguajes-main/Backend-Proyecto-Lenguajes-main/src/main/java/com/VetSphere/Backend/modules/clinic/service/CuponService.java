package com.VetSphere.Backend.modules.clinic.service;

import com.VetSphere.Backend.exception.BusinessException;
import com.VetSphere.Backend.exception.ResourceNotFoundException;
import com.VetSphere.Backend.modules.clinic.domain.CanjeCupon;
import com.VetSphere.Backend.modules.clinic.domain.Cupon;
import com.VetSphere.Backend.modules.clinic.domain.Factura;
import com.VetSphere.Backend.modules.clinic.domain.Producto;
import com.VetSphere.Backend.modules.clinic.dto.CuponRequest;
import com.VetSphere.Backend.modules.clinic.dto.LineaFacturable;
import com.VetSphere.Backend.modules.clinic.repository.CanjeCuponRepository;
import com.VetSphere.Backend.modules.clinic.repository.CuponRepository;
import com.VetSphere.Backend.modules.clinic.repository.ProductoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

/**
 * Gestion de cupones de descuento de la farmacia.
 *
 * Reglas:
 * <ul>
 *   <li>El importe del descuento SIEMPRE se calcula en el servidor; el cliente
 *       solo envia el codigo.</li>
 *   <li>El cupon puede acotarse a un producto, a una categoria o ser global.</li>
 *   <li>Se respeta la vigencia, el limite de usos y la compra minima.</li>
 *   <li>Cada canje queda registrado para poder auditar que descuento se aplico.</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class CuponService {
    private static final SecureRandom ALEATORIO = new SecureRandom();
    private static final String ALFABETO = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final BigDecimal CIEN = new BigDecimal("100");

    private final CuponRepository cuponRepository;
    private final CanjeCuponRepository canjeRepository;
    private final ProductoRepository productoRepository;
    private final AuditoriaService auditoriaService;

    /**
     * Resultado de aplicar un cupon: el cupon usado y el importe del descuento
     * ya calculado sobre la base que le corresponde.
     */
    public record AplicacionCupon(Cupon cupon, String codigo, BigDecimal descuento) {
        public static AplicacionCupon vacio() {
            return new AplicacionCupon(null, null, BigDecimal.ZERO);
        }

        public boolean aplicado() {
            return cupon != null && descuento != null && descuento.signum() > 0;
        }
    }

    // ------------------------------------------------------------------
    // CRUD
    // ------------------------------------------------------------------

    public List<Cupon> listar() {
        return cuponRepository.findAllByOrderByFechaCreacionDesc();
    }

    public List<Cupon> vigentes() {
        return cuponRepository.findVigentes(LocalDateTime.now());
    }

    public Cupon consultar(Long id) {
        return cuponRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cupon no encontrado: " + id));
    }

    @Transactional
    public Cupon crear(CuponRequest request) {
        Cupon cupon = new Cupon();
        cupon.setCodigo(generarCodigo(request.codigo()));
        cupon.setFechaCreacion(LocalDateTime.now());
        cupon.setOrigen(Cupon.ORIGEN_MANUAL);
        cupon.setCreadoPor(usuarioActual());
        aplicarDatos(cupon, request);
        Cupon guardado = cuponRepository.save(cupon);
        auditoriaService.registrar(AuditoriaService.CUPON_CREADO, "Cupon", guardado.getId(),
                "Cupon " + guardado.getCodigo() + " (" + guardado.getTipoDescuento() + " " + guardado.getValor() + ")");
        return guardado;
    }

    @Transactional
    public Cupon actualizar(Long id, CuponRequest request) {
        Cupon cupon = consultar(id);
        if (request.codigo() != null && !request.codigo().isBlank()) {
            String codigo = request.codigo().trim().toUpperCase(Locale.ROOT);
            cuponRepository.findByCodigoIgnoreCase(codigo)
                    .filter(otro -> !otro.getId().equals(id))
                    .ifPresent(otro -> { throw new BusinessException("Ya existe un cupon con el codigo " + codigo); });
            cupon.setCodigo(codigo);
        }
        aplicarDatos(cupon, request);
        return cuponRepository.save(cupon);
    }

    @Transactional
    public Cupon activar(Long id, boolean activo) {
        Cupon cupon = consultar(id);
        cupon.setActivo(activo);
        Cupon guardado = cuponRepository.save(cupon);
        auditoriaService.registrar(AuditoriaService.CUPON_CREADO, "Cupon", id,
                "Cupon " + cupon.getCodigo() + (activo ? " activado" : " desactivado"));
        return guardado;
    }

    @Transactional
    public void eliminar(Long id) {
        Cupon cupon = consultar(id);
        long canjes = canjeRepository.countByCuponId(id);
        if (canjes > 0) {
            // No se borra un cupon ya usado: se desactiva para preservar la trazabilidad.
            cupon.setActivo(false);
            cuponRepository.save(cupon);
            throw new BusinessException("El cupon ya fue usado en " + canjes
                    + " factura(s), por lo que se desactivo en lugar de eliminarse.");
        }
        cuponRepository.delete(cupon);
    }

    private void aplicarDatos(Cupon cupon, CuponRequest request) {
        String tipo = request.tipoDescuento() == null ? Cupon.PORCENTAJE
                : request.tipoDescuento().trim().toUpperCase(Locale.ROOT);
        if (!Cupon.PORCENTAJE.equals(tipo) && !Cupon.VALOR_FIJO.equals(tipo)) {
            throw new BusinessException("Tipo de descuento no valido: use PORCENTAJE o VALOR_FIJO.");
        }
        BigDecimal valor = request.valor() == null ? BigDecimal.ZERO : request.valor();
        if (valor.signum() < 0) {
            throw new BusinessException("El valor del descuento no puede ser negativo.");
        }
        if (Cupon.PORCENTAJE.equals(tipo) && valor.compareTo(CIEN) > 0) {
            throw new BusinessException("El descuento porcentual no puede superar el 100%.");
        }
        cupon.setTipoDescuento(tipo);
        cupon.setValor(valor.setScale(2, RoundingMode.HALF_UP));
        cupon.setDescripcion(request.descripcion());
        cupon.setCategoria(request.categoria() == null || request.categoria().isBlank()
                ? null : request.categoria().trim());
        cupon.setMinimoSubtotal(request.minimoSubtotal());
        cupon.setUsosMaximos(request.usosMaximos());
        cupon.setActivo(request.activo() == null ? Boolean.TRUE : request.activo());
        if (cupon.getUsosActuales() == null) {
            cupon.setUsosActuales(0);
        }
        cupon.setFechaInicio(request.fechaInicio() == null ? LocalDateTime.now() : request.fechaInicio());
        cupon.setFechaFin(request.fechaFin() == null
                ? cupon.getFechaInicio().plusDays(30) : request.fechaFin());
        if (!cupon.getFechaFin().isAfter(cupon.getFechaInicio())) {
            throw new BusinessException("La fecha de fin debe ser posterior a la fecha de inicio.");
        }
        if (request.productoCodigo() != null && !request.productoCodigo().isBlank()) {
            Producto producto = productoRepository.findByCodigo(request.productoCodigo().trim())
                    .orElseThrow(() -> new BusinessException("Producto no encontrado: " + request.productoCodigo()));
            cupon.setProducto(producto);
        } else {
            cupon.setProducto(null);
        }
    }

    // ------------------------------------------------------------------
    // Aplicacion del cupon en la venta
    // ------------------------------------------------------------------

    /**
     * Valida el cupon y calcula el descuento sobre la base que le corresponde.
     * Lanza BusinessException con un mensaje claro si el cupon no es aplicable.
     */
    public AplicacionCupon validarYCalcular(String codigo, List<LineaFacturable> lineas) {
        if (codigo == null || codigo.isBlank()) {
            return AplicacionCupon.vacio();
        }
        Cupon cupon = cuponRepository.findByCodigoIgnoreCase(codigo.trim())
                .orElseThrow(() -> new BusinessException("El cupon " + codigo.trim().toUpperCase(Locale.ROOT)
                        + " no existe."));
        LocalDateTime ahora = LocalDateTime.now();
        if (!Boolean.TRUE.equals(cupon.getActivo())) {
            throw new BusinessException("El cupon " + cupon.getCodigo() + " no esta activo.");
        }
        if (cupon.getFechaInicio() != null && cupon.getFechaInicio().isAfter(ahora)) {
            throw new BusinessException("El cupon " + cupon.getCodigo() + " aun no esta vigente.");
        }
        if (cupon.getFechaFin() != null && cupon.getFechaFin().isBefore(ahora)) {
            throw new BusinessException("El cupon " + cupon.getCodigo() + " ya expiro.");
        }
        long usos = canjeRepository.countByCuponId(cupon.getId());
        if (cupon.getUsosMaximos() != null && usos >= cupon.getUsosMaximos()) {
            throw new BusinessException("El cupon " + cupon.getCodigo() + " alcanzo su limite de usos.");
        }
        BigDecimal base = baseAplicable(cupon, lineas);
        if (base.signum() <= 0) {
            throw new BusinessException("El cupon " + cupon.getCodigo()
                    + " no aplica a los productos de esta factura.");
        }
        if (cupon.getMinimoSubtotal() != null && base.compareTo(cupon.getMinimoSubtotal()) < 0) {
            throw new BusinessException("El cupon " + cupon.getCodigo() + " requiere una compra minima de "
                    + cupon.getMinimoSubtotal().setScale(2, RoundingMode.HALF_UP) + ".");
        }
        BigDecimal descuento;
        if (Cupon.VALOR_FIJO.equals(cupon.getTipoDescuento())) {
            descuento = cupon.getValor().min(base);
        } else {
            descuento = base.multiply(cupon.getValor())
                    .divide(CIEN, 2, RoundingMode.HALF_UP);
        }
        if (descuento.signum() <= 0) {
            throw new BusinessException("El cupon " + cupon.getCodigo() + " no genera descuento.");
        }
        return new AplicacionCupon(cupon, cupon.getCodigo(), descuento.setScale(2, RoundingMode.HALF_UP));
    }

    /** Importe sobre el que se aplica el cupon segun su alcance. */
    private BigDecimal baseAplicable(Cupon cupon, List<LineaFacturable> lineas) {
        List<LineaFacturable> seguras = lineas == null ? List.of() : lineas;
        if (cupon.getProducto() != null) {
            String codigo = cupon.getProducto().getCodigo();
            return seguras.stream()
                    .filter(l -> codigo != null && codigo.equalsIgnoreCase(l.productoCodigo()))
                    .map(LineaFacturable::importe)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }
        if (cupon.getCategoria() != null && !cupon.getCategoria().isBlank()) {
            String categoria = cupon.getCategoria();
            return seguras.stream()
                    .filter(l -> categoria.equalsIgnoreCase(l.categoria()))
                    .map(LineaFacturable::importe)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }
        return seguras.stream().map(LineaFacturable::importe).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** Registra el canje del cupon sobre una factura. */
    @Transactional
    public CanjeCupon registrarCanje(Cupon cupon, Factura factura, BigDecimal descuentoAplicado) {
        CanjeCupon canje = canjeRepository.save(CanjeCupon.builder()
                .cupon(cupon)
                .factura(factura)
                .codigo(cupon.getCodigo())
                .descuentoAplicado(descuentoAplicado)
                .fecha(LocalDateTime.now())
                .usuarioEmail(usuarioActual())
                .build());
        cupon.setUsosActuales((int) canjeRepository.countByCuponId(cupon.getId()));
        cuponRepository.save(cupon);
        auditoriaService.registrar(AuditoriaService.CUPON_CANJEADO, "Cupon", cupon.getId(),
                "Cupon " + cupon.getCodigo() + " aplicado por " + descuentoAplicado
                        + " en la factura " + (factura == null ? "?" : factura.getNumero()));
        return canje;
    }

    /**
     * Crea un cupon automatico acaparado a un producto de baja rotacion.
     * El codigo es legible (PROMO-&lt;codigo del producto&gt;) para que el personal
     * de farmacia pueda escribirlo sin erratas.
     */
    @Transactional
    public Cupon crearParaBajaRotacion(Producto producto, BigDecimal descuentoPorcentaje,
                                       int duracionDias, String descripcion) {
        if (producto == null) {
            throw new BusinessException("El producto es obligatorio para crear la promocion.");
        }
        if (descuentoPorcentaje == null || descuentoPorcentaje.signum() <= 0) {
            throw new BusinessException("El descuento de la promocion debe ser mayor a 0.");
        }
        BigDecimal porcentaje = descuentoPorcentaje.min(CIEN).setScale(2, RoundingMode.HALF_UP);
        LocalDateTime inicio = LocalDateTime.now();
        Cupon cupon = new Cupon();
        cupon.setCodigo(codigoParaProducto(producto, porcentaje));
        cupon.setDescripcion(descripcion == null || descripcion.isBlank()
                ? "Promocion de baja rotacion para " + producto.getNombre() : descripcion);
        cupon.setTipoDescuento(Cupon.PORCENTAJE);
        cupon.setValor(porcentaje);
        cupon.setProducto(producto);
        cupon.setCategoria(producto.getCategoria());
        cupon.setFechaInicio(inicio);
        cupon.setFechaFin(inicio.plusDays(Math.max(1, duracionDias)));
        cupon.setUsosMaximos(Math.max(1, producto.getStock() == null ? 1 : producto.getStock()));
        cupon.setUsosActuales(0);
        cupon.setActivo(true);
        cupon.setOrigen(Cupon.ORIGEN_ROTACION);
        cupon.setCreadoPor(usuarioActual());
        cupon.setFechaCreacion(inicio);
        Cupon guardado = cuponRepository.save(cupon);
        auditoriaService.registrar(AuditoriaService.CUPON_CREADO, "Cupon", guardado.getId(),
                "Promocion automatica " + guardado.getCodigo() + " (" + porcentaje + "% sobre "
                        + producto.getCodigo() + ")");
        return guardado;
    }

    /** Codigo unico y legible para la promocion de un producto. */
    private String codigoParaProducto(Producto producto, BigDecimal porcentaje) {
        String base = ("PROMO-" + producto.getCodigo()).toUpperCase(Locale.ROOT)
                .replaceAll("[^A-Z0-9-]", "-");
        if (base.length() > 24) {
            base = base.substring(0, 24);
        }
        String codigo = base + "-" + porcentaje.setScale(0, RoundingMode.HALF_UP).toPlainString();
        if (codigo.length() > 32) {
            codigo = codigo.substring(0, 32);
        }
        String candidato = codigo;
        int sufijo = 2;
        while (cuponRepository.findByCodigoIgnoreCase(candidato).isPresent()) {
            sufijo++;
            String extra = "-" + sufijo;
            candidato = codigo.length() + extra.length() > 32
                    ? codigo.substring(0, 32 - extra.length()) + extra
                    : codigo + extra;
        }
        return candidato;
    }

    /** Genera un codigo unico legible para el cupon. */
    private String generarCodigo(String sugerido) {        if (sugerido != null && !sugerido.isBlank()) {
            String codigo = sugerido.trim().toUpperCase(Locale.ROOT);
            if (!codigo.matches("[A-Z0-9-]{3,32}")) {
                throw new BusinessException("El codigo solo admite letras, numeros y guiones (3 a 32 caracteres).");
            }
            if (cuponRepository.findByCodigoIgnoreCase(codigo).isPresent()) {
                throw new BusinessException("Ya existe un cupon con el codigo " + codigo + ".");
            }
            return codigo;
        }
        for (int intento = 0; intento < 20; intento++) {
            StringBuilder sb = new StringBuilder("VS-");
            for (int i = 0; i < 6; i++) {
                sb.append(ALFABETO.charAt(ALEATORIO.nextInt(ALFABETO.length())));
            }
            String codigo = sb.toString();
            if (cuponRepository.findByCodigoIgnoreCase(codigo).isEmpty()) {
                return codigo;
            }
        }
        throw new BusinessException("No se pudo generar un codigo de cupon unico. Intenta de nuevo.");
    }

    private String usuarioActual() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication == null ? null : authentication.getName();
    }
}
