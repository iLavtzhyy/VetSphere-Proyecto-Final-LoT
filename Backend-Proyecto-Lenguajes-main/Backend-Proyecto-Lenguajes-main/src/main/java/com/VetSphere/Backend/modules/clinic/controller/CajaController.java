package com.VetSphere.Backend.modules.clinic.controller;

import com.VetSphere.Backend.modules.clinic.domain.Factura;
import com.VetSphere.Backend.modules.clinic.repository.FacturaRepository;
import com.VetSphere.Backend.shared.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Cierre de caja: los importes se calculan con las facturas reales, nunca con
 * cifras fijas. Se consideran las facturas emitidas (no anuladas) del dia.
 */
@RestController
@RequestMapping("/api/caja")
@RequiredArgsConstructor
public class CajaController {
    private final FacturaRepository facturaRepository;

    @GetMapping("/cierre")
    public ApiResponse<Map<String, Object>> cierre() {
        List<Factura> facturas = facturaRepository.findAll();
        LocalDate hoy = LocalDate.now();

        List<Factura> emitidas = facturas.stream()
                .filter(f -> f.getFecha() != null && f.getFecha().toLocalDate().equals(hoy))
                .toList();
        List<Factura> anuladas = emitidas.stream()
                .filter(f -> "Anulado".equalsIgnoreCase(f.getEstadoPago()))
                .toList();

        BigDecimal totalHoy = emitidas.stream()
                .filter(f -> !"Anulado".equalsIgnoreCase(f.getEstadoPago()))
                .map(f -> f.getTotal() == null ? BigDecimal.ZERO : f.getTotal())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, Object> respuesta = new LinkedHashMap<>();
        respuesta.put("fecha", hoy);
        respuesta.put("facturasEmitidas", emitidas.size());
        respuesta.put("facturasAnuladas", anuladas.size());
        respuesta.put("facturasTotales", facturas.size());
        respuesta.put("totalEstimado", totalHoy);
        respuesta.put("pagadas", emitidas.stream().filter(f -> "Pagado".equalsIgnoreCase(f.getEstadoPago())).count());
        respuesta.put("pendientes", emitidas.stream().filter(f -> "Pendiente".equalsIgnoreCase(f.getEstadoPago())).count());
        return ApiResponse.ok("Cierre de caja", respuesta);
    }
}
