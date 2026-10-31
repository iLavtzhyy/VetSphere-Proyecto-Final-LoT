package com.VetSphere.Backend.modules.clinic.controller;

import com.VetSphere.Backend.modules.clinic.domain.OrdenCompra;
import com.VetSphere.Backend.modules.clinic.dto.OrdenCompraRequest;
import com.VetSphere.Backend.modules.clinic.service.OrdenCompraService;
import com.VetSphere.Backend.shared.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * CRUD de las ordenes de compra (persistidas en base de datos).
 * Antes era un facade con datos de demostracion hardcodeados.
 */
@Tag(name = "Compras", description = "Gestion de las ordenes de compra de la clinica (solo ADMIN)")
@RestController
@RequestMapping("/api/compras")
@RequiredArgsConstructor
public class CompraController {
    private final OrdenCompraService ordenCompraService;

    @GetMapping("/ordenes")
    @Operation(summary = "Lista las ordenes de compra ordenadas por fecha descendente")
    public ApiResponse<List<OrdenCompra>> ordenes() {
        return ApiResponse.ok("Ordenes de compra", ordenCompraService.listar());
    }

    @PostMapping("/ordenes")
    @Operation(summary = "Crea una orden de compra (el numero lo genera el servidor)")
    public ApiResponse<OrdenCompra> crear(@Valid @RequestBody OrdenCompraRequest request) {
        return ApiResponse.ok("Orden de compra registrada", ordenCompraService.crear(request));
    }

    @PutMapping("/ordenes/{id}")
    @Operation(summary = "Actualiza una orden de compra existente")
    public ApiResponse<OrdenCompra> actualizar(@PathVariable Long id, @Valid @RequestBody OrdenCompraRequest request) {
        return ApiResponse.ok("Orden de compra actualizada", ordenCompraService.actualizar(id, request));
    }

    @DeleteMapping("/ordenes/{id}")
    @Operation(summary = "Elimina una orden de compra")
    public ApiResponse<Void> eliminar(@PathVariable Long id) {
        ordenCompraService.eliminar(id);
        return ApiResponse.ok("Orden de compra eliminada", null);
    }
}
