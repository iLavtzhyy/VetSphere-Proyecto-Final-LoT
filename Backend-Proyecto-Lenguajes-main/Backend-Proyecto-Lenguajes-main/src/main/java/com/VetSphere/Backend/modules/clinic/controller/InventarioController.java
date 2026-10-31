package com.VetSphere.Backend.modules.clinic.controller;

import com.VetSphere.Backend.modules.clinic.domain.MovimientoInventario;
import com.VetSphere.Backend.modules.clinic.repository.MovimientoInventarioRepository;
import com.VetSphere.Backend.shared.response.ApiResponse;
import com.VetSphere.Backend.shared.response.Paginacion;
import com.VetSphere.Backend.modules.clinic.service.ProductoService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;

/**
 * Inventario general: consulta completa del catalogo y del historial de
 * movimientos de stock registrados por ProductoService.
 */
@RestController
@RequestMapping("/api/inventario")
@RequiredArgsConstructor
public class InventarioController {
    private final ProductoService productoService;
    private final MovimientoInventarioRepository movimientoRepository;

    @GetMapping
    @Operation(summary = "Inventario completo (solo ADMIN; paginacion opcional con page/size)")
    public ApiResponse<Object> inventarioCompleto(@RequestParam(required = false) Integer page,
                                                  @RequestParam(required = false) Integer size) {
        var productos = productoService.listar();
        if (Paginacion.solicitada(page, size)) {
            return Paginacion.responder("Inventario completo", productos, page, size);
        }
        return ApiResponse.ok("Inventario completo", productos);
    }

    @GetMapping("/movimientos")
    @Operation(summary = "Historial de movimientos de inventario (ADMIN o RECEPCIONISTA)")
    public ApiResponse<List<MovimientoInventario>> movimientos() {
        return ApiResponse.ok("Movimientos de inventario", movimientoRepository.findTop50ByOrderByFechaDesc());
    }
}
