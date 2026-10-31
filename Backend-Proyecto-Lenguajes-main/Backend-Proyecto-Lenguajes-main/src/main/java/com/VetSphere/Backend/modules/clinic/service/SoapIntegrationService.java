package com.VetSphere.Backend.modules.clinic.service;

import com.VetSphere.Backend.exception.BusinessException;
import com.VetSphere.Backend.security.SoapSecurityCallbackHandler;
import com.VetSphere.Backend.soap.dto.ProductoSoapResponse;
import com.VetSphere.Backend.soap.dto.StockUpdateResponse;
import com.VetSphere.Backend.soap.endpoint.ProductoSoapService;
import lombok.RequiredArgsConstructor;
import org.apache.cxf.frontend.ClientProxy;
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;
import org.apache.cxf.ws.security.wss4j.WSS4JOutInterceptor;
import org.apache.wss4j.common.ConfigurationConstants;
import org.apache.wss4j.dom.WSConstants;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SoapIntegrationService {
    private final SoapSecurityCallbackHandler passwordCallbackHandler;

    @Value("${vetsphere.soap.url}")
    private String endpoint;

    @Value("${vetsphere.soap.security.username}")
    private String username;

    public List<ProductoSoapResponse> consultarStockCritico() {
        ProductoSoapService client = crearCliente();
        try {
            List<ProductoSoapResponse> products = client.stockCritico();
            return products == null ? List.of() : products;
        } catch (RuntimeException ex) {
            throw new BusinessException("No se pudo completar la consulta protegida de inventario.");
        } finally {
            ClientProxy.getClient(client).destroy();
        }
    }

    public ProductoSoapResponse consultarProducto(String codigo) {
        ProductoSoapService client = crearCliente();
        try { return client.consultarProducto(codigo); }
        catch (RuntimeException ex) { throw new BusinessException("No se pudo consultar el producto por SOAP."); }
        finally { ClientProxy.getClient(client).destroy(); }
    }

    public StockUpdateResponse agregarStock(String codigo, int cantidad) {
        ProductoSoapService client = crearCliente();
        try { return client.agregarStock(codigo, cantidad); }
        catch (RuntimeException ex) { throw new BusinessException("No se pudo agregar stock mediante SOAP."); }
        finally { ClientProxy.getClient(client).destroy(); }
    }

    public StockUpdateResponse descontarStock(String codigo, int cantidad) {
        ProductoSoapService client = crearCliente();
        try { return client.descontarStock(codigo, cantidad); }
        catch (RuntimeException ex) { throw new BusinessException("No se pudo descontar stock mediante SOAP."); }
        finally { ClientProxy.getClient(client).destroy(); }
    }

    public StockUpdateResponse actualizarStock(String codigo, int stock) {
        ProductoSoapService client = crearCliente();
        try { return client.actualizarStock(codigo, stock); }
        catch (RuntimeException ex) { throw new BusinessException("No se pudo fijar el stock mediante SOAP."); }
        finally { ClientProxy.getClient(client).destroy(); }
    }

    private ProductoSoapService crearCliente() {
        JaxWsProxyFactoryBean factory = new JaxWsProxyFactoryBean();
        factory.setServiceClass(ProductoSoapService.class);
        factory.setAddress(endpoint);
        Map<String, Object> security = new HashMap<>();
        security.put(ConfigurationConstants.ACTION, ConfigurationConstants.USERNAME_TOKEN);
        security.put(ConfigurationConstants.USER, username);
        security.put(ConfigurationConstants.PASSWORD_TYPE, WSConstants.PW_DIGEST);
        security.put(ConfigurationConstants.PW_CALLBACK_REF, passwordCallbackHandler);
        factory.getOutInterceptors().add(new WSS4JOutInterceptor(security));
        return (ProductoSoapService) factory.create();
    }
}
