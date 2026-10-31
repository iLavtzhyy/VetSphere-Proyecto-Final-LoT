package com.vetsphere.mini.controller;

import com.vetsphere.mini.domain.ProductoMini;
import com.vetsphere.mini.repository.ProductoMiniRepository;
import com.vetsphere.mini.service.SoapClientService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/stock")
@RequiredArgsConstructor
public class StockController {
    private static final Logger log = LoggerFactory.getLogger(StockController.class);
    private final ProductoMiniRepository repository;
    private final SoapClientService soapClient;

    @GetMapping("/productos")
    public List<Map<String, Object>> getProductos() {
        List<ProductoMini> locales = repository.findAll();
        List<Map<String, Object>> resultado = new ArrayList<>();

        for (ProductoMini p : locales) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", p.getId());
            map.put("codigo", p.getCodigo());
            map.put("nombre", p.getNombre());
            map.put("stockLocal", p.getStock());
            
            // Consultar stock remoto por SOAP
            int stockRemoto = soapClient.consultarStockRemoto(p.getCodigo());
            map.put("stockRemoto", stockRemoto == -1 ? "Error/Desconectado" : stockRemoto);
            
            resultado.add(map);
        }
        return resultado;
    }

    @PostMapping("/transferir")
    @Transactional
    public ResponseEntity<?> transferir(
            @RequestParam String codigo,
            @RequestParam int cantidad,
            @RequestParam String direccion) {
        
        if (cantidad <= 0) {
            return ResponseEntity.badRequest().body(Map.of("error", "La cantidad debe ser mayor a 0."));
        }
        if (codigo == null || !codigo.matches("[A-Za-z0-9_-]{1,64}")) {
            return ResponseEntity.badRequest().body(Map.of("error", "Código de producto inválido."));
        }

        ProductoMini producto = repository.findByCodigoParaActualizar(codigo)
                .orElse(null);
        if (producto == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Producto no registrado localmente."));
        }

        try {
            if ("enviar".equalsIgnoreCase(direccion)) {
                // Enviar de Local -> Remoto (Quita de local, añade a remoto)
                if (producto.getStock() < cantidad) {
                    return ResponseEntity.badRequest().body(Map.of("error", "Stock local insuficiente."));
                }
                
                // 1. Llamar a SOAP para añadir a remoto
                soapClient.agregarStockRemoto(codigo, cantidad);

                try {
                    producto.setStock(producto.getStock() - cantidad);
                    repository.saveAndFlush(producto);
                } catch (RuntimeException errorLocal) {
                    try {
                        soapClient.descontarStockRemoto(codigo, cantidad);
                    } catch (Exception errorCompensacion) {
                        log.error("No fue posible compensar el envío; requiere reconciliación manual para {}", codigo, errorCompensacion);
                    }
                    throw errorLocal;
                }

            } else if ("recibir".equalsIgnoreCase(direccion)) {
                // Traer de Remoto -> Local (Quita de remoto, añade a local)
                int stockRemoto = soapClient.consultarStockRemoto(codigo);
                if (stockRemoto == -1) {
                    return ResponseEntity.status(500).body(Map.of("error", "No se pudo conectar con el servidor SOAP de VetSphere."));
                }
                if (stockRemoto < cantidad) {
                    return ResponseEntity.badRequest().body(Map.of("error", "Stock en VetSphere principal es insuficiente."));
                }

                // 1. Llamar a SOAP para restar del remoto
                int nuevoStockLocal;
                try {
                    nuevoStockLocal = Math.addExact(producto.getStock(), cantidad);
                } catch (ArithmeticException overflow) {
                    return ResponseEntity.badRequest().body(Map.of("error", "El stock resultante excede el rango permitido."));
                }
                soapClient.descontarStockRemoto(codigo, cantidad);

                try {
                    producto.setStock(nuevoStockLocal);
                    repository.saveAndFlush(producto);
                } catch (RuntimeException errorLocal) {
                    try {
                        soapClient.agregarStockRemoto(codigo, cantidad);
                    } catch (Exception errorCompensacion) {
                        log.error("No fue posible compensar la recepción; requiere reconciliación manual para {}", codigo, errorCompensacion);
                    }
                    throw errorLocal;
                }
                
            } else {
                return ResponseEntity.badRequest().body(Map.of("error", "Direccion de transferencia invalida."));
            }

            return ResponseEntity.ok(Map.of("mensaje", "Transferencia completada con éxito."));

        } catch (Exception e) {
            // La llamada SOAP y la BD local no comparten una transacción distribuida.
            // Si algo falla, fuerza rollback local; las compensaciones remotas quedan registradas.
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            log.warn("Falló la transferencia de inventario entre sistemas", e);
            return ResponseEntity.status(502).body(Map.of("error", "No se pudo completar la transferencia; verifica ambos inventarios antes de reintentar."));
        }
    }
}
