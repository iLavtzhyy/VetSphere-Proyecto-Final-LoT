package com.VetSphere.Backend.modules.clinic.controller;

import com.VetSphere.Backend.modules.clinic.domain.Cupon;
import com.VetSphere.Backend.modules.clinic.dto.CuponAutoRequest;
import com.VetSphere.Backend.modules.clinic.dto.CuponRequest;
import com.VetSphere.Backend.modules.clinic.dto.CuponSimulacionRequest;
import com.VetSphere.Backend.modules.clinic.dto.FacturaRequest;
import com.VetSphere.Backend.shared.response.ApiResponse;
import com.VetSphere.Backend.modules.clinic.service.CuponService;
import com.VetSphere.Backend.modules.clinic.service.FacturacionService;
import com.VetSphere.Backend.modules.clinic.service.InventarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Analisis de rotacion de inventario y gestion de cupones de descuento.
 *
 * El analisis responde con datos reales de venta que productos no se estan
 * vendiendo y, a partir de ese resultado, genera automaticamente cupones
 * de descuento para empujarlos.
 */
@Tag(name = "Farmacia - Promociones",
        description = "Rotacion de inventario, deteccion de baja rotacion y cupones de descuento")
@RestController
@RequestMapping("/api/farmacia")
@RequiredArgsConstructor
public class FarmaciaPromocionesController {
    private final InventarioService inventarioService;
    private final CuponService cuponService;
    private final FacturacionService facturacionService;

    // ------------------------------------------------------------------
    // Rotacion de inventario
    // ------------------------------------------------------------------

    @GetMapping("/rotacion")
    @Operation(summary = "Analiza que productos no se venden (rotacion, cobertura y capital dormido)")
    public ApiResponse<Map<String, Object>> rotacion(
            @RequestParam(required = false) Integer dias,
            @RequestParam(required = false) Integer umbral) {
        return ApiResponse.ok("Analisis de rotacion de inventario",
                inventarioService.analizarRotacion(dias, umbral));
    }

    @PostMapping("/cupones/auto")
    @Operation(summary = "Genera cupones de descuento para los productos de baja rotacion")
    public ApiResponse<Map<String, Object>> generarAutomaticos(
            @RequestBody(required = false) CuponAutoRequest request) {
        CuponAutoRequest efectivo = request == null
                ? new CuponAutoRequest(null, null, null, null)
                : request;
        return ApiResponse.ok("Promociones generadas para productos de baja rotacion",
                inventarioService.generarCuponesBajaRotacion(efectivo));
    }

    // ------------------------------------------------------------------
    // Cupones
    // ------------------------------------------------------------------

    @PostMapping("/cupones/validar")
    @Operation(summary = "Simula el descuento de un cupon sobre el carrito de venta")
    public ApiResponse<Map<String, Object>> validarCupon(@Valid @RequestBody CuponSimulacionRequest request) {
        // La simulacion reutiliza el mismo modelo de la factura: concepto, subtotal,
        // lineas del carrito y codigo del cupon. El resto de campos se deja vacio.
        FacturaRequest simulacion = new FacturaRequest(
                null,                                                   // numero
                null,                                                   // fecha
                request.concepto() == null || request.concepto().isBlank()
                        ? "Simulacion de venta en farmacia" : request.concepto(),
                request.subtotal(),                                      // subtotal
                null, null,                                              // impuestos, total
                null, null,                                              // estadoPago, metodoPago
                null, null,                                              // cliente, mascota
                null, null, null, null,                                  // consultaIds, cirugiaIds, laboratorioIds, hospitalizacionIds
                request.items(),                                         // items
                request.codigo(),                                        // cuponCodigo
                null);                                                   // observaciones
        return ApiResponse.ok("Simulacion del cupon", facturacionService.previsualizarCupon(simulacion));
    }

    @GetMapping("/cupones")
    @Operation(summary = "Lista los cupones de descuento")
    public ApiResponse<List<Cupon>> listarCupones(
            @RequestParam(required = false, defaultValue = "false") boolean soloVigentes) {
        List<Cupon> cupones = soloVigentes ? cuponService.vigentes() : cuponService.listar();
        return ApiResponse.ok("Cupones de descuento", cupones);
    }

    @GetMapping("/cupones/{id}")
    @Operation(summary = "Consulta un cupon por id")
    public ApiResponse<Cupon> consultarCupon(@PathVariable Long id) {
        return ApiResponse.ok("Cupon", cuponService.consultar(id));
    }

    @PostMapping("/cupones")
    @Operation(summary = "Crea un cupon de descuento")
    public ApiResponse<Cupon> crearCupon(@Valid @RequestBody CuponRequest request) {
        return ApiResponse.ok("Cupon creado", cuponService.crear(request));
    }

    @PutMapping("/cupones/{id}")
    @Operation(summary = "Actualiza un cupon de descuento")
    public ApiResponse<Cupon> actualizarCupon(@PathVariable Long id, @Valid @RequestBody CuponRequest request) {
        return ApiResponse.ok("Cupon actualizado", cuponService.actualizar(id, request));
    }

    @PutMapping("/cupones/{id}/estado")
    @Operation(summary = "Activa o desactiva un cupon")
    public ApiResponse<Cupon> estadoCupon(@PathVariable Long id, @RequestBody Map<String, Boolean> datos) {
        boolean activo = Boolean.TRUE.equals(datos.get("activo"));
        return ApiResponse.ok(activo ? "Cupon activado" : "Cupon desactivado",
                cuponService.activar(id, activo));
    }

    @DeleteMapping("/cupones/{id}")
    @Operation(summary = "Elimina un cupon (si ya fue usado, se desactiva)")
    public ApiResponse<Void> eliminarCupon(@PathVariable Long id) {
        cuponService.eliminar(id);
        return ApiResponse.ok("Cupon eliminado", null);
    }
}
