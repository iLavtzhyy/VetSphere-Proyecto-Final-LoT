package com.VetSphere.Backend.config;

import com.VetSphere.Backend.soap.endpoint.ProductoSoapEndpoint;
import com.VetSphere.Backend.security.SoapSecurityCallbackHandler;
import jakarta.xml.ws.Endpoint;
import lombok.RequiredArgsConstructor;
import org.apache.cxf.Bus;
import org.apache.cxf.jaxws.EndpointImpl;
import org.apache.cxf.ws.security.wss4j.WSS4JInInterceptor;
import org.apache.wss4j.common.ConfigurationConstants;
import org.apache.wss4j.dom.WSConstants;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
@RequiredArgsConstructor
public class CxfSoapConfig {
    private final Bus bus;
    private final ProductoSoapEndpoint productoSoapEndpoint;
    private final SoapSecurityCallbackHandler soapSecurityCallbackHandler;

    @Bean
    public Endpoint productoEndpoint() {
        EndpointImpl endpoint = new EndpointImpl(bus, productoSoapEndpoint);
        Map<String, Object> seguridad = new HashMap<>();
        seguridad.put(ConfigurationConstants.ACTION, ConfigurationConstants.USERNAME_TOKEN);
        seguridad.put(ConfigurationConstants.PASSWORD_TYPE, WSConstants.PW_DIGEST);
        seguridad.put(ConfigurationConstants.PW_CALLBACK_REF, soapSecurityCallbackHandler);
        endpoint.getInInterceptors().add(new WSS4JInInterceptor(seguridad));
        endpoint.getProperties().put("security.enable.streaming", true);
        endpoint.publish("/productos");
        return endpoint;
    }
}


