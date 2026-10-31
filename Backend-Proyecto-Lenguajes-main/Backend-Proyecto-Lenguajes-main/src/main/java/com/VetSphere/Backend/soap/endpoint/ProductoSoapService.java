package com.VetSphere.Backend.soap.endpoint;

import com.VetSphere.Backend.soap.dto.ProductoSoapResponse;
import com.VetSphere.Backend.soap.dto.StockUpdateResponse;
import jakarta.jws.WebMethod;
import jakarta.jws.WebParam;
import jakarta.jws.WebService;

import java.util.List;

@WebService(targetNamespace = "https://soap.vetsphere.com/productos", name = "ProductoSoapService")
public interface ProductoSoapService {
    @WebMethod
    ProductoSoapResponse consultarProducto(@WebParam(name = "codigo") String codigo);

    @WebMethod
    StockUpdateResponse agregarStock(@WebParam(name = "codigo") String codigo, @WebParam(name = "cantidad") int cantidad);

    @WebMethod
    StockUpdateResponse descontarStock(@WebParam(name = "codigo") String codigo, @WebParam(name = "cantidad") int cantidad);

    @WebMethod
    StockUpdateResponse actualizarStock(@WebParam(name = "codigo") String codigo, @WebParam(name = "stock") int stock);

    @WebMethod
    List<ProductoSoapResponse> stockCritico();
}
