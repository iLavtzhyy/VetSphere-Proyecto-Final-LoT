package com.VetSphere.Backend.modules.clinic.controller;

import com.VetSphere.Backend.modules.clinic.domain.Producto;
import com.VetSphere.Backend.shared.response.ApiResponse;
import com.VetSphere.Backend.shared.response.Paginacion;
import com.VetSphere.Backend.modules.clinic.service.ProductoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "Farmacia", description = "Inventario de productos y movimientos de stock")
@RestController
@RequestMapping("/api/farmacia")
@RequiredArgsConstructor
public class FarmaciaController {
    private final ProductoService productoService;

    @GetMapping("/inventario")
    @Operation(summary = "Lista el inventario de la farmacia (paginacion opcional con page/size)")
    public ApiResponse<Object> inventario(@RequestParam(required = false) Integer page,
                                          @RequestParam(required = false) Integer size) {
        List<Producto> productos = productoService.listar();
        if (Paginacion.solicitada(page, size)) {
            return Paginacion.responder("Inventario farmacia", productos, page, size);
        }
        return ApiResponse.ok("Inventario farmacia", productos);
    }

    @GetMapping("/criticos")
    @Operation(summary = "Productos con stock en nivel critico")
    public ApiResponse<List<Producto>> criticos() {
        return ApiResponse.ok("Medicamentos criticos", productoService.stockCritico());
    }

    @PostMapping("/inventario")
    @Operation(summary = "Registra un producto nuevo")
    public ApiResponse<Producto> crear(@RequestBody Producto producto) {
        return ApiResponse.ok("Producto registrado", productoService.guardar(producto));
    }

    @PutMapping("/inventario/{codigo}")
    @Operation(summary = "Actualiza un producto y registra el movimiento de ajuste")
    public ApiResponse<Producto> actualizar(@PathVariable String codigo, @RequestBody Producto producto) {
        return ApiResponse.ok("Producto actualizado", productoService.actualizar(codigo, producto));
    }

    @PutMapping("/inventario/{codigo}/stock")
    @Operation(summary = "Ajusta directamente el stock de un producto")
    public ApiResponse<Producto> actualizarStock(@PathVariable String codigo, @RequestBody Map<String, Integer> datos) {
        return ApiResponse.ok("Stock actualizado", productoService.actualizarStock(codigo, datos.getOrDefault("stock", 0)));
    }

    @PostMapping("/inventario/{codigo}/movimiento")
    @Operation(summary = "Agrega stock (entrada de inventario)")
    public ApiResponse<Producto> agregarStock(@PathVariable String codigo, @RequestBody Map<String, Integer> datos) {
        return ApiResponse.ok("Movimiento de stock aplicado", productoService.agregarStock(codigo, datos.getOrDefault("cantidad", 0)));
    }

    @PostMapping("/inventario/{codigo}/descontar")
    @Operation(summary = "Descuenta stock (salida de inventario)")
    public ApiResponse<Producto> descontarStock(@PathVariable String codigo, @RequestBody Map<String, Integer> datos) {
        return ApiResponse.ok("Stock descontado", productoService.descontarStock(codigo, datos.getOrDefault("cantidad", 0)));
    }
}
