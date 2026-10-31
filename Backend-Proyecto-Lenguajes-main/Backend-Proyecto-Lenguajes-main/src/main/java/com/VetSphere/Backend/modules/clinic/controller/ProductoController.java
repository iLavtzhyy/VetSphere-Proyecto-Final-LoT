package com.VetSphere.Backend.modules.clinic.controller;

import com.VetSphere.Backend.modules.clinic.domain.Producto;
import com.VetSphere.Backend.shared.response.ApiResponse;
import com.VetSphere.Backend.modules.clinic.service.ProductoService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/productos")
@RequiredArgsConstructor
public class ProductoController {
    private final ProductoService productoService;

    @GetMapping
    public ApiResponse<List<Producto>> listar() {
        return ApiResponse.ok("Productos", productoService.listar());
    }

    @GetMapping("/stock-critico")
    public ApiResponse<List<Producto>> stockCritico() {
        return ApiResponse.ok("Stock critico", productoService.stockCritico());
    }
}


