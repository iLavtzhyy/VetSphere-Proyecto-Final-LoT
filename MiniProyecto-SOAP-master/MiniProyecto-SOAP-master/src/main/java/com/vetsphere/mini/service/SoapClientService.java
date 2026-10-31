package com.vetsphere.mini.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.ZonedDateTime;
import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class SoapClientService {
    private static final Logger log = LoggerFactory.getLogger(SoapClientService.class);
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    @Value("${vetsphere.soap.url}")
    private String soapUrl;

    @Value("${vetsphere.soap.username}")
    private String soapUser;

    @Value("${vetsphere.soap.password}")
    private String soapPass;

    public int consultarStockRemoto(String codigo) {
        try {
            validarCodigo(codigo);
            String bodyContent = "<prod:consultarProducto><codigo>" + codigo + "</codigo></prod:consultarProducto>";
            String responseXml = enviarSoap(bodyContent);
            return extraerStock(responseXml);
        } catch (Exception e) {
            log.warn("No fue posible consultar el inventario remoto");
            return -1; // Indica error
        }
    }

    public int agregarStockRemoto(String codigo, int cantidad) throws Exception {
        validarCodigo(codigo);
        if (cantidad <= 0) throw new IllegalArgumentException("La cantidad debe ser positiva.");
        String bodyContent = "<prod:agregarStock><codigo>" + codigo + "</codigo><cantidad>" + cantidad + "</cantidad></prod:agregarStock>";
        return extraerEntero(enviarSoap(bodyContent), "stockActual");
    }

    public int descontarStockRemoto(String codigo, int cantidad) throws Exception {
        validarCodigo(codigo);
        if (cantidad <= 0) throw new IllegalArgumentException("La cantidad debe ser positiva.");
        String bodyContent = "<prod:descontarStock><codigo>" + codigo + "</codigo><cantidad>" + cantidad + "</cantidad></prod:descontarStock>";
        return extraerEntero(enviarSoap(bodyContent), "stockActual");
    }

    private String enviarSoap(String bodyContent) throws Exception {
        String envelope = buildSoapEnvelope(bodyContent);
        HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(soapUrl))
                .header("Content-Type", "text/xml;charset=UTF-8")
                .timeout(Duration.ofSeconds(10))
                .POST(HttpRequest.BodyPublishers.ofString(envelope, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new RuntimeException("El servicio SOAP rechazó la operación (HTTP " + response.statusCode() + ").");
        }
        return response.body();
    }

    private void validarCodigo(String codigo) {
        if (codigo == null || !codigo.matches("[A-Za-z0-9_-]{1,64}")) {
            throw new IllegalArgumentException("Código de producto inválido.");
        }
    }

    private String buildSoapEnvelope(String bodyContent) {
        // Generar Nonce aleatorio de 16 bytes
        byte[] nonceBytes = new byte[16];
        SECURE_RANDOM.nextBytes(nonceBytes);
        String nonceBase64 = Base64.getEncoder().encodeToString(nonceBytes);

        // Marca de tiempo de creacion UTC ISO-8601
        String created = ZonedDateTime.now(java.time.ZoneOffset.UTC)
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"));

        // Calcular el Password Digest
        String passwordDigest;
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            md.update(nonceBytes);
            md.update(created.getBytes(StandardCharsets.UTF_8));
            md.update(soapPass.getBytes(StandardCharsets.UTF_8));
            passwordDigest = Base64.getEncoder().encodeToString(md.digest());
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo generar el token SOAP.", e);
        }

        return "<soapenv:Envelope xmlns:soapenv=\"https://schemas.xmlsoap.org/soap/envelope/\"\n" +
                "                  xmlns:prod=\"https://soap.vetsphere.com/productos\">\n" +
                "   <soapenv:Header>\n" +
                "      <wsse:Security xmlns:wsse=\"https://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd\"\n" +
                "                     xmlns:wsu=\"https://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd\">\n" +
                "         <wsse:UsernameToken>\n" +
                "            <wsse:Username>" + escaparXml(soapUser) + "</wsse:Username>\n" +
                "            <wsse:Password Type=\"https://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-username-token-profile-1.0#PasswordDigest\">" + passwordDigest + "</wsse:Password>\n" +
                "            <wsse:Nonce EncodingType=\"https://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-soap-message-security-1.0#Base64Binary\">" + nonceBase64 + "</wsse:Nonce>\n" +
                "            <wsu:Created>" + created + "</wsu:Created>\n" +
                "         </wsse:UsernameToken>\n" +
                "      </wsse:Security>\n" +
                "   </soapenv:Header>\n" +
                "   <soapenv:Body>\n" +
                "      " + bodyContent + "\n" +
                "   </soapenv:Body>\n" +
                "</soapenv:Envelope>";
    }

    private int extraerStock(String responseXml) {
        return extraerEntero(responseXml, "stock");
    }

    private int extraerEntero(String responseXml, String elemento) {
        Pattern pattern = Pattern.compile("<(?:[\\w.-]+:)?" + elemento + "[^>]*>(\\d+)</(?:[\\w.-]+:)?" + elemento + ">", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(responseXml);
        if (matcher.find()) {
            return Integer.parseInt(matcher.group(1));
        }
        throw new IllegalStateException("El servicio SOAP no devolvió el stock actualizado.");
    }

    private String escaparXml(String valor) {
        return valor.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&apos;");
    }
}
