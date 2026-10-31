package com.VetSphere.Backend.modules.clinic.controller;

import com.VetSphere.Backend.modules.clinic.domain.Factura;
import com.VetSphere.Backend.modules.clinic.dto.FacturaRequest;
import com.VetSphere.Backend.modules.clinic.repository.FacturaRepository;
import com.VetSphere.Backend.modules.clinic.service.FacturacionService;
import com.VetSphere.Backend.modules.clinic.service.FacturaPdfService;
import com.VetSphere.Backend.shared.response.ApiResponse;
import com.VetSphere.Backend.shared.response.Paginacion;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Endpoints de facturacion. La logica (y en especial el recalculo de subtotal,
 * IVA y total en el servidor) vive en {@link FacturacionService}; la descarga
 * en PDF vive en {@link FacturaPdfService}.
 */
@Tag(name = "Facturacion", description = "Emision y estado de pago de facturas")
@RestController
@RequestMapping({"/api/facturacion", "/api/facturas"})
@RequiredArgsConstructor
public class FacturacionController {
    private final FacturacionService facturacionService;
    private final FacturaRepository facturaRepository;
    private final FacturaPdfService facturaPdfService;
    private final com.VetSphere.Backend.modules.clinic.service.CuentaHospitalizacionService cuentaHospitalizacionService;

    @GetMapping("/datos-pago")
    @Operation(summary = "Datos de transferencia de la clinica")
    public ApiResponse<Map<String, Object>> datosPago() {
        return ApiResponse.ok("Datos de pago", cuentaHospitalizacionService.datosPago());
    }

    @GetMapping
    @Operation(summary = "Lista facturas (paginacion opcional con page/size)")
    public ApiResponse<Object> listar(@RequestParam(required = false) Integer page,
                                      @RequestParam(required = false) Integer size) {
        if (Paginacion.solicitada(page, size)) {
            int pagina = Paginacion.pagina(page);
            int tamano = Paginacion.tamano(size);
            var resultado = facturaRepository.findAll(PageRequest.of(pagina, tamano));
            return Paginacion.responder("Facturas", facturacionService.mapear(resultado.getContent()),
                    resultado.getTotalElements(), page, size);
        }
        return Paginacion.responder("Facturas", facturacionService.listar(), facturacionService.total(), null, null);
    }

    @PostMapping
    @Operation(summary = "Emite una factura (subtotal, IVA 15% y total se recalculan en el servidor)")
    public ApiResponse<Map<String, Object>> crear(@Valid @RequestBody FacturaRequest request) {
        return ApiResponse.ok("Factura emitida", facturacionService.crearMapeada(request));
    }

    @PutMapping("/{id}/estado")
    @Operation(summary = "Actualiza el estado de pago de una factura")
    public ApiResponse<Map<String, Object>> actualizarEstado(@PathVariable Long id, @RequestBody Map<String, String> request) {
        return ApiResponse.ok("Estado de pago actualizado", facturacionService.actualizarEstadoMapeado(id, request));
    }

    /**
     * Descarga el PDF de una factura (GET /api/facturas/{id}/pdf). Si la factura
     * no existe se lanza ResourceNotFoundException y el GlobalExceptionHandler
     * responde 404 con el envoltorio ApiResponse.
     */
    @GetMapping("/{id}/pdf")
    @Operation(summary = "Descarga la factura en PDF (roles ADMIN y RECEPCIONISTA)")
    public ResponseEntity<byte[]> descargarPdf(@PathVariable Long id) {
        FacturaPdfService.FacturaPdf pdf = facturaPdfService.generarPdf(id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + pdf.nombreArchivo())
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf.contenido());
    }
}
