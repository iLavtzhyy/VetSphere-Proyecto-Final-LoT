package com.VetSphere.Backend.soap.endpoint;

import com.VetSphere.Backend.soap.dto.ProductoSoapResponse;
import com.VetSphere.Backend.soap.dto.StockUpdateResponse;
import com.VetSphere.Backend.modules.clinic.domain.Producto;
import com.VetSphere.Backend.modules.clinic.service.ProductoService;
import jakarta.jws.WebService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@WebService(endpointInterface = "com.VetSphere.Backend.soap.endpoint.ProductoSoapService",
        serviceName = "ProductoSoapService",
        targetNamespace = "https://soap.vetsphere.com/productos")
public class ProductoSoapEndpoint implements ProductoSoapService {
    private final ProductoService productoService;

    @Override
    public ProductoSoapResponse consultarProducto(String codigo) {
        return map(productoService.consultar(codigo));
    }

    @Override
    public StockUpdateResponse agregarStock(String codigo, int cantidad) {
        Producto producto = productoService.agregarStock(codigo, cantidad);
        return new StockUpdateResponse(codigo, producto.getStock(), "Stock agregado correctamente");
    }

    @Override
    public StockUpdateResponse descontarStock(String codigo, int cantidad) {
        Producto producto = productoService.descontarStock(codigo, cantidad);
        return new StockUpdateResponse(codigo, producto.getStock(), "Stock descontado correctamente");
    }

    @Override
    public StockUpdateResponse actualizarStock(String codigo, int stock) {
        Producto producto = productoService.actualizarStock(codigo, stock);
        return new StockUpdateResponse(codigo, producto.getStock(), "Stock actualizado correctamente");
    }

    @Override
    public List<ProductoSoapResponse> stockCritico() {
        return productoService.stockCritico().stream().map(this::map).toList();
    }

    private ProductoSoapResponse map(Producto producto) {
        ProductoSoapResponse response = new ProductoSoapResponse();
        response.setCodigo(producto.getCodigo());
        response.setNombre(producto.getNombre());
        response.setCategoria(producto.getCategoria());
        response.setStock(producto.getStock());
        response.setStockCritico(producto.getStockCritico());
        response.setPrecio(producto.getPrecio());
        response.setPrioridadAlta(producto.isPrioridadAlta());
        response.setCritico(producto.getStock() <= producto.getStockCritico());
        return response;
    }
}

