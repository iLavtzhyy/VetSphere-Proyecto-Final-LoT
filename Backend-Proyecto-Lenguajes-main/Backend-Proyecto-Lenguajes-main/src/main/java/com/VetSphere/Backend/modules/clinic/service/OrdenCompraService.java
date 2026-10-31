package com.VetSphere.Backend.modules.clinic.service;

import com.VetSphere.Backend.exception.BusinessException;
import com.VetSphere.Backend.exception.ResourceNotFoundException;
import com.VetSphere.Backend.modules.clinic.domain.OrdenCompra;
import com.VetSphere.Backend.modules.clinic.domain.Proveedor;
import com.VetSphere.Backend.modules.clinic.domain.Usuario;
import com.VetSphere.Backend.modules.clinic.dto.OrdenCompraRequest;
import com.VetSphere.Backend.modules.clinic.repository.OrdenCompraRepository;
import com.VetSphere.Backend.modules.clinic.repository.ProveedorRepository;
import com.VetSphere.Backend.modules.clinic.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Gestion de las ordenes de compra de la clinica.
 *
 * Reglas de negocio:
 * <ul>
 *   <li>El proveedor indicado debe existir en el sistema.</li>
 *   <li>El numero de orden lo genera el servidor (OC-&lt;anio&gt;-&lt;consecutivo&gt;)
 *       salvo que el cliente lo envie, en cuyo caso no puede estar repetido.</li>
 *   <li>El estado solo puede ser Solicitada, Recibida, Cancelada o Completada.</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class OrdenCompraService {
    /** Estados admitidos para una orden de compra. */
    private static final List<String> ESTADOS = List.of(
            OrdenCompra.SOLICITADA,
            OrdenCompra.RECIBIDA,
            OrdenCompra.CANCELADA,
            OrdenCompra.COMPLETADA);

    private final OrdenCompraRepository ordenCompraRepository;
    private final ProveedorRepository proveedorRepository;
    private final UsuarioRepository usuarioRepository;

    /** Lista las ordenes de compra ordenadas por fecha descendente. */
    public List<OrdenCompra> listar() {
        return ordenCompraRepository.findAllByOrderByFechaDesc();
    }

    public OrdenCompra crear(OrdenCompraRequest request) {
        Proveedor proveedor = obtenerProveedor(request.proveedorId());
        Usuario responsable = obtenerResponsable(request.responsableId());

        return ordenCompraRepository.save(OrdenCompra.builder()
                .numero(resolverNumero(request.numero(), null))
                .proveedor(proveedor)
                .fecha(request.fecha())
                .estado(resolverEstado(request.estado(), OrdenCompra.SOLICITADA))
                .total(request.total() == null ? BigDecimal.ZERO : request.total())
                .responsable(responsable)
                .observaciones(request.observaciones())
                .detalle(request.detalle())
                .build());
    }

    public OrdenCompra actualizar(Long id, OrdenCompraRequest request) {
        OrdenCompra orden = ordenCompraRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("La orden de compra no existe"));
        if (request.proveedorId() != null) {
            orden.setProveedor(obtenerProveedor(request.proveedorId()));
        }
        if (request.responsableId() != null) {
            orden.setResponsable(obtenerResponsable(request.responsableId()));
        }
        orden.setNumero(resolverNumero(request.numero(), orden));
        if (request.fecha() != null) {
            orden.setFecha(request.fecha());
        }
        if (request.estado() != null && !request.estado().isBlank()) {
            orden.setEstado(resolverEstado(request.estado(), orden.getEstado()));
        }
        if (request.total() != null) {
            orden.setTotal(request.total());
        }
        orden.setObservaciones(request.observaciones());
        orden.setDetalle(request.detalle());
        return ordenCompraRepository.save(orden);
    }

    public void eliminar(Long id) {
        if (!ordenCompraRepository.existsById(id)) {
            throw new ResourceNotFoundException("La orden de compra no existe");
        }
        ordenCompraRepository.deleteById(id);
    }

    /** Localiza el proveedor destinatario de la orden. */
    private Proveedor obtenerProveedor(Long proveedorId) {
        if (proveedorId == null) {
            throw new BusinessException("Debes indicar el proveedor de la orden.");
        }
        return proveedorRepository.findById(proveedorId)
                .orElseThrow(() -> new BusinessException("El proveedor indicado no existe en el sistema."));
    }

    /** Localiza al usuario responsable de la orden (opcional). */
    private Usuario obtenerResponsable(Long responsableId) {
        if (responsableId == null) {
            return null;
        }
        return usuarioRepository.findById(responsableId)
                .orElseThrow(() -> new BusinessException("El responsable indicado no existe en el sistema."));
    }

    /**
     * Determina el numero de la orden. Si el cliente lo envia se valida que no
     * este repetido; si no, se genera el correlativo del ano en curso.
     */
    private String resolverNumero(String solicitado, OrdenCompra ordenExistente) {
        if (solicitado != null && !solicitado.isBlank()) {
            String numero = solicitado.trim();
            if (ordenExistente != null && numero.equalsIgnoreCase(ordenExistente.getNumero())) {
                return ordenExistente.getNumero();
            }
            if (ordenCompraRepository.existsByNumero(numero)) {
                throw new BusinessException("Ya existe una orden de compra con el numero " + numero + ".");
            }
            return numero;
        }
        if (ordenExistente != null && ordenExistente.getNumero() != null && !ordenExistente.getNumero().isBlank()) {
            return ordenExistente.getNumero();
        }
        return generarNumero();
    }

    /**
     * Genera el siguiente numero de orden del ano en curso con el formato
     * OC-&lt;anio&gt;-&lt;consecutivo&gt;, tomando como base las ordenes ya
     * registradas (asi se evita repetir un numero aunque se borren ordenes).
     */
    private String generarNumero() {
        String prefijo = "OC-" + LocalDate.now().getYear() + "-";
        int consecutivo = siguienteConsecutivo(prefijo);
        String numero = prefijo + String.format("%04d", consecutivo);
        while (ordenCompraRepository.existsByNumero(numero)) {
            consecutivo++;
            numero = prefijo + String.format("%04d", consecutivo);
        }
        return numero;
    }

    /** Mayor consecutivo usado para el prefijo dado, mas uno. */
    private int siguienteConsecutivo(String prefijo) {
        int consecutivo = 1;
        for (OrdenCompra orden : ordenCompraRepository.findByNumeroStartingWith(prefijo)) {
            String numero = orden.getNumero();
            if (numero == null || !numero.startsWith(prefijo)) {
                continue;
            }
            try {
                int valor = Integer.parseInt(numero.substring(prefijo.length()).trim());
                if (valor >= consecutivo) {
                    consecutivo = valor + 1;
                }
            } catch (NumberFormatException ignored) {
                // Numeros con otro formato no participan del correlativo.
            }
        }
        return consecutivo;
    }

    /** Valida el estado de la orden contra los estados permitidos. */
    private String resolverEstado(String estado, String porDefecto) {
        if (estado == null || estado.isBlank()) {
            return porDefecto == null ? OrdenCompra.SOLICITADA : porDefecto;
        }
        String limpio = estado.trim();
        for (String permitido : ESTADOS) {
            if (permitido.equalsIgnoreCase(limpio)) {
                return permitido;
            }
        }
        throw new BusinessException("El estado de la orden debe ser uno de: "
                + String.join(", ", ESTADOS) + ".");
    }
}
