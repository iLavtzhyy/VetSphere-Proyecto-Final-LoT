# VetSphere - Documentacion tecnica

## Backend

- Framework: Spring Boot con Maven.
- Java configurado: release 17 para compilar estable incluso usando Temurin 25 en IntelliJ.
- Base de datos: SQL Server `VeterinariaDB`.
- Paquete principal: `com.VetSphere.Backend`.
- Carpetas principales: `config`, `exception`, `modules`, `security`, `shared`, `soap`.

## Seguridad

- REST protegido con Spring Security.
- Autenticacion por JWT.
- Contrasenas encriptadas con BCrypt.
- Rutas administrativas protegidas por `ROLE_ADMIN`.
- Procesamiento XML protegido contra XXE y expansion de entidades en `XmlSecurityConfig`.
- SOAP protegido con Apache CXF y WS-Security UsernameToken Digest.

## Roles

- `ROLE_ADMIN`: usuarios, reportes, facturacion y administracion.
- `ROLE_VETERINARIO`: consultas, historial clinico, cirugias, laboratorio y hospitalizacion.
- `ROLE_RECEPCIONISTA`: citas, clientes y admision.
- `ROLE_CLIENTE`: acceso de tutor para sus datos y mascotas.

## Correo real

El registro, recuperacion de contrasena y verificacion usan Spring Mail.

En `application-local.properties` o variables de entorno configura:

```properties
MAIL_USERNAME=tu-correo@gmail.com
MAIL_PASSWORD=tu-password-de-aplicacion
```

Google requiere una contrasena de aplicacion, no la contrasena normal de Gmail.

## SOAP/CXF

WSDL:

```text
http://localhost:8080/soap/productos?wsdl
```

Servicio:

```text
ProductoSoapEndpoint
```

Ejemplo de prueba XML:

```xml
<soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/"
                  xmlns:prod="http://soap.vetsphere.com/productos">
  <soapenv:Header>
    <wsse:Security>
      <wsse:UsernameToken>
        <wsse:Username>vetsphere-soap</wsse:Username>
        <wsse:Password Type="PasswordDigest">***</wsse:Password>
      </wsse:UsernameToken>
    </wsse:Security>
  </soapenv:Header>
  <soapenv:Body>
    <prod:actualizarStock>
      <codigo>MED-028</codigo>
      <stock>30</stock>
    </prod:actualizarStock>
  </soapenv:Body>
</soapenv:Envelope>
```

## Reportes

- PDF de historial clinico: `/api/reportes/mascotas/{id}/historial.pdf`
- Excel financiero: `/api/reportes/finanzas.xlsx`
- Comprobante de pago: desde la pantalla `Facturacion`.

## Frontend

- Angular con componentes separados por carpeta.
- Cada pantalla tiene `.ts`, `.html`, `.css` y `.spec.ts`.
- Ruta de exposicion: `/calidad-software` (solo rol ADMIN).
