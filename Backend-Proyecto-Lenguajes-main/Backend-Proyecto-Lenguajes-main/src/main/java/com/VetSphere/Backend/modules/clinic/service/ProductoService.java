package com.VetSphere.Backend.modules.clinic.service;

import com.VetSphere.Backend.exception.BusinessException;
import com.VetSphere.Backend.exception.ResourceNotFoundException;
import com.VetSphere.Backend.modules.clinic.domain.MovimientoInventario;
import com.VetSphere.Backend.modules.clinic.domain.Producto;
import com.VetSphere.Backend.modules.clinic.repository.MovimientoInventarioRepository;
import com.VetSphere.Backend.modules.clinic.repository.ProductoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductoService {
    private final ProductoRepository productoRepository;
    private final MovimientoInventarioRepository movimientoRepository;
    private final AuditoriaService auditoriaService;

    public List<Producto> listar() {
        return productoRepository.findAll();
    }

    /**
     * Productos con stock por debajo del minimo critico.
     * Se revisa TODOS los productos, no solo los marcados como prioritarios:
     * de lo contrario el contador del panel no cuadra con el endpoint de criticos.
     */
    public List<Producto> stockCritico() {
        return productoRepository.findAll().stream()
                .filter(Producto::disponible)
                .filter(p -> p.getStock() != null && p.getStockCritico() != null
                        && p.getStock() <= p.getStockCritico())
                .toList();
    }

    public Producto consultar(String codigo) {
        return productoRepository.findByCodigo(codigo)
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado: " + codigo));
    }

    @Transactional
    public Producto guardar(Producto producto) {
        // El identificador lo asigna el servidor: un alta jamas sobrescribe otro producto.
        producto.setId(null);
        if (producto.getCodigo() == null || producto.getCodigo().isBlank()) {
            throw new BusinessException("El codigo del producto es obligatorio.");
        }
        String codigo = producto.getCodigo().trim().toUpperCase();
        productoRepository.findByCodigo(codigo).ifPresent(existente -> {
            throw new BusinessException("Ya existe un producto con el codigo " + codigo + ".");
        });
        producto.setCodigo(codigo);
        if (producto.getNombre() == null || producto.getNombre().isBlank()) {
            throw new BusinessException("El nombre del producto es obligatorio.");
        }
        if (producto.getPrecio() == null || producto.getPrecio().signum() < 0) {
            throw new BusinessException("El precio del producto es invalido.");
        }
        if (producto.getStock() == null || producto.getStock() < 0) {
            throw new BusinessException("El stock del producto no puede ser negativo.");
        }
        validarDescuento(producto.getDescuento());
        producto.setActivo(producto.getActivo() == null ? Boolean.TRUE : producto.getActivo());
        Producto guardado = productoRepository.save(producto);
        auditoriaService.registrar(AuditoriaService.PRODUCTO_CREADO, "Producto", guardado.getId(),
                "Alta de producto " + guardado.getCodigo() + " (" + guardado.getNombre() + ")");
        return guardado;
    }

    @Transactional
    public Producto actualizar(String codigo, Producto datos) {
        Producto producto = consultar(codigo);
        producto.setNombre(datos.getNombre());
        producto.setCategoria(datos.getCategoria());
        int stockAnterior = producto.getStock() == null ? 0 : producto.getStock();
        producto.setStock(datos.getStock());
        producto.setStockCritico(datos.getStockCritico());
        producto.setPrecio(datos.getPrecio());
        producto.setPrioridadAlta(datos.isPrioridadAlta());
        producto.setDescuento(datos.getDescuento());
        producto.setActivo(datos.getActivo() == null ? producto.getActivo() : datos.getActivo());
        producto.setLote(datos.getLote());
        producto.setFechaVencimiento(datos.getFechaVencimiento());
        validarDescuento(producto.getDescuento());
        Producto guardado = productoRepository.save(producto);
        int stockNuevo = guardado.getStock() == null ? 0 : guardado.getStock();
        if (stockAnterior != stockNuevo) {
            registrarMovimiento(guardado, MovimientoInventario.AJUSTE, Math.abs(stockNuevo - stockAnterior),
                    stockAnterior, stockNuevo, "Actualizacion completa del producto");
        } else {
            auditoriaService.registrar(AuditoriaService.PRODUCTO_ACTUALIZADO, "Producto", guardado.getId(),
                    "Actualizacion de producto " + guardado.getCodigo());
        }
        return guardado;
    }

    private void validarDescuento(BigDecimal descuento) {
        if (descuento == null) {
            return;
        }
        if (descuento.signum() < 0 || descuento.compareTo(new BigDecimal("100")) > 0) {
            throw new BusinessException("El descuento debe estar entre 0% y 100%.");
        }
    }

    @Transactional
    public Producto agregarStock(String codigo, int cantidad) {
        if (cantidad <= 0) throw new BusinessException("La cantidad a agregar debe ser positiva.");
        Producto producto = buscarParaActualizar(codigo);
        if (producto.getStock() == null || producto.getStock() > Integer.MAX_VALUE - cantidad) {
            throw new BusinessException("El stock resultante excede el rango permitido.");
        }
        int stockAnterior = producto.getStock();
        producto.setStock(stockAnterior + cantidad);
        Producto guardado = productoRepository.save(producto);
        registrarMovimiento(guardado, MovimientoInventario.ENTRADA, cantidad, stockAnterior, guardado.getStock(),
                "Entrada de stock manual");
        return guardado;
    }

    @Transactional
    public Producto descontarStock(String codigo, int cantidad) {
        return descontar(codigo, cantidad, MovimientoInventario.SALIDA, "Salida de stock manual");
    }

    /** Descuenta existencias con trazabilidad del paciente y la estancia clinica. */
    @Transactional
    public Producto descontarStockPorConsumo(String codigo, int cantidad, String motivo) {
        return descontar(codigo, cantidad, MovimientoInventario.SALIDA,
                motivo == null || motivo.isBlank() ? "Consumo clinico" : motivo);
    }

    /**
     * Descuenta el stock como consecuencia de una venta. El movimiento queda
     * tipificado como VENTA, lo que permite distinguirlo de una salida manual
     * (merma, consumo interno, devolucion).
     */
    @Transactional
    public Producto descontarStockPorVenta(String codigo, int cantidad, String motivo) {
        return descontar(codigo, cantidad, MovimientoInventario.VENTA,
                motivo == null || motivo.isBlank() ? "Venta registrada en factura" : motivo);
    }

    private Producto descontar(String codigo, int cantidad, String tipo, String motivo) {
        if (cantidad <= 0) throw new BusinessException("La cantidad a descontar debe ser positiva.");
        Producto producto = buscarParaActualizar(codigo);
        if (producto.getStock() == null || producto.getStock() < cantidad) {
            throw new BusinessException("Stock insuficiente en VetSphere: " + codigo + ".");
        }
        int stockAnterior = producto.getStock();
        producto.setStock(stockAnterior - cantidad);
        Producto guardado = productoRepository.save(producto);
        registrarMovimiento(guardado, tipo, cantidad, stockAnterior, guardado.getStock(), motivo);
        return guardado;
    }

    @Transactional
    public Producto actualizarStock(String codigo, int stock) {
        if (stock < 0) throw new BusinessException("El stock no puede ser negativo.");
        Producto producto = consultar(codigo);
        int stockAnterior = producto.getStock() == null ? 0 : producto.getStock();
        producto.setStock(stock);
        Producto guardado = productoRepository.save(producto);
        registrarMovimiento(guardado, MovimientoInventario.AJUSTE, Math.abs(stock - stockAnterior),
                stockAnterior, stock, "Ajuste directo de stock");
        return guardado;
    }

    /** Persiste el cambio de stock aplicado por SOAP, API o interfaz. */
    private void registrarMovimiento(Producto producto, String tipo, int cantidad, int stockAnterior,
                                     int stockNuevo, String motivo) {
        movimientoRepository.save(MovimientoInventario.builder()
                .fecha(LocalDateTime.now())
                .producto(producto.getNombre() + " (" + producto.getCodigo() + ")")
                .productoId(producto.getId())
                .tipo(tipo)
                .cantidad(cantidad)
                .stockAnterior(stockAnterior)
                .stockNuevo(stockNuevo)
                .motivo(motivo)
                .usuarioEmail(usuarioActual())
                .build());
        auditoriaService.registrar(AuditoriaService.STOCK_MODIFICADO, "Producto", producto.getId(),
                producto.getCodigo() + ": " + stockAnterior + " -> " + stockNuevo + " (" + tipo + ")");
    }

    private String usuarioActual() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication == null ? null : authentication.getName();
    }

    private Producto buscarParaActualizar(String codigo) {
        return productoRepository.findByCodigoParaActualizar(codigo)
                .orElseThrow(() -> new BusinessException("Producto no encontrado."));
    }
}
