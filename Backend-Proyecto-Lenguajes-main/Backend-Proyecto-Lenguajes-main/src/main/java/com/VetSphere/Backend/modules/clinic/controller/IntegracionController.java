package com.VetSphere.Backend.modules.clinic.controller;

import com.VetSphere.Backend.modules.clinic.service.SoapIntegrationService;
import com.VetSphere.Backend.shared.response.ApiResponse;
import com.VetSphere.Backend.soap.dto.ProductoSoapResponse;
import com.VetSphere.Backend.soap.dto.StockUpdateResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/integraciones")
@RequiredArgsConstructor
public class IntegracionController {
    private final SoapIntegrationService soapIntegrationService;

    @GetMapping("/inventario")
    public ApiResponse<List<ProductoSoapResponse>> verificarInventario() {
        return ApiResponse.ok("Consulta de inventario completada", soapIntegrationService.consultarStockCritico());
    }

    @GetMapping("/inventario/{codigo}")
    public ApiResponse<ProductoSoapResponse> consultarProducto(@PathVariable String codigo) {
        return ApiResponse.ok("Producto consultado por SOAP", soapIntegrationService.consultarProducto(codigo));
    }

    @PostMapping("/inventario/{codigo}/movimiento")
    public ApiResponse<StockUpdateResponse> agregarStock(@PathVariable String codigo, @RequestBody Map<String, Integer> datos) {
        return ApiResponse.ok("Stock agregado por SOAP", soapIntegrationService.agregarStock(codigo, datos.getOrDefault("cantidad", 0)));
    }

    @PostMapping("/inventario/{codigo}/descontar")
    public ApiResponse<StockUpdateResponse> descontarStock(@PathVariable String codigo, @RequestBody Map<String, Integer> datos) {
        return ApiResponse.ok("Stock descontado por SOAP", soapIntegrationService.descontarStock(codigo, datos.getOrDefault("cantidad", 0)));
    }

    @PutMapping("/inventario/{codigo}/stock")
    public ApiResponse<StockUpdateResponse> actualizarStock(@PathVariable String codigo, @RequestBody Map<String, Integer> datos) {
        return ApiResponse.ok("Stock actualizado por SOAP", soapIntegrationService.actualizarStock(codigo, datos.getOrDefault("stock", 0)));
    }
}
