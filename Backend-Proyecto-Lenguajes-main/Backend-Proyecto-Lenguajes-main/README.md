# VetSphere · Backend

API REST + servicio SOAP del sistema de información clínica veterinaria **VetSphere**.

**Stack:** Spring Boot 3.3.5 · Java 17 · Spring Security (JWT) · Spring Data JPA · Apache CXF + WSS4J (SOAP) · SQL Server · Apache POI (Excel) · OpenPDF · springdoc-openapi · JUnit 5 + Mockito + H2 · JaCoCo

---

## ▶️ Arranque rápido

```powershell
# 1. Crear la base de datos (VeterinariaDB y VeterinariaMiniDB)
.\scripts\Preparar-SqlServer.ps1

# 2. Configurar secretos
Copy-Item .env.example .env   # y completar valores

# 3. Ejecutar (carga .env automáticamente y usa mvn spring-boot:run)
.\iniciar-backend.ps1
```

| Recurso | URL |
|---|---|
| API REST | `http://localhost:8080/api` |
| Swagger UI (ADMIN) | `http://localhost:8080/swagger-ui.html` |
| Documentación OpenAPI | `http://localhost:8080/v3/api-docs` |
| WSDL SOAP | `http://localhost:8080/soap/productos?wsdl` |

Requisitos: JDK 17+, Maven (o IntelliJ con Maven embebido), SQL Server. Variables de entorno: ver `.env.example` y la [guía de instalación](../docs/INSTALACION.md).

---

## 🗂️ Estructura

```
src/main/java/com/VetSphere/Backend/
├── VetSphereBackendApplication.java
├── config/          → SecurityConfig, CORS, CXF (SOAP), uploads, sembrador de demo
├── security/        → JwtService, JwtAuthenticationFilter, handler WS-Security
├── exception/       → BusinessException, ResourceNotFoundException, GlobalExceptionHandler
├── shared/response/ → ApiResponse<T> (envoltorio {ok, mensaje, data})
├── modules/clinic/
│   ├── controller/  → 38+ controladores REST
│   ├── service/     → lógica de negocio transaccional (facturación, tarifas, chat, auditoría…)
│   ├── repository/  → Spring Data JPA
│   ├── domain/      → entidades JPA (Usuario, Mascota, HistorialClinico, Cirugia, Cita, Factura, Producto…)
│   └── dto/         → peticiones/respuestas con validación jakarta
└── soap/            → endpoint JAX-WS ProductoSoapService/Endpoint
```

**Convención de respuestas:** todos los endpoints devuelven `{ ok, mensaje, data }`.

---

## 🔑 Roles

`ROLE_ADMIN` · `ROLE_VETERINARIO` · `ROLE_RECEPCIONISTA` · `ROLE_CLIENTE`

La autorización se aplica en `config/SecurityConfig.java` por URL; los clientes solo acceden a sus propias mascotas y reportes. Módulos administrativos (auditoría, indicadores, Swagger, proveedores, configuración) restringidos a ADMIN.

---

## 🧾 Servicio SOAP

- Endpoint: `/soap/productos` (Apache CXF), operaciones: `consultarProducto`, `agregarStock`, `descontarStock`, `actualizarStock`, `stockCritico`.
- Seguridad: **WS-Security UsernameToken + PasswordDigest** (nonce + SHA-1), validado en tiempo constante.
- Esquema: `src/main/resources/soap/productos.xsd`.
- Integración REST→SOAP: `service/SoapIntegrationService` (usada por `/api/integraciones/inventario`).
- Prueba manual desde la raíz del repo: `python test_soap.py` (con `SOAP_PASSWORD` en el entorno).

---

## ✅ Pruebas

```powershell
$env:JAVA_HOME="C:\Program Files\JetBrains\IntelliJ IDEA <versión>\jbr"
& "C:\Program Files\JetBrains\IntelliJ IDEA <versión>\plugins\maven-plugin\lib\maven3\bin\mvn.cmd" verify
```

- **Unitarios** (Mockito): JwtService, FacturacionService (recalculo de IVA), ProductoService (stock y movimientos), TarifaService, ChatService.
- **Integración** (`@SpringBootTest` + MockMvc + H2 en memoria, profile `test`): login/registro/2FA, autorización 403 por rol, CRUD de mascotas, facturación, inventario y chat.
- Cobertura: **JaCoCo** → `target/site/jacoco/index.html`.

---

## 🔒 Seguridad

- JWT HS256 (30 min) + revalidación de roles por petición; 401 con envoltorio estándar para tokens inválidos.
- BCrypt para contraseñas; verificación por código de 6 dígitos y recuperación por correo.
- Validación de entrada, manejo centralizado de excepciones sin stacktraces, endurecimiento XML (anti-XXE).
- Archivos clínicos subidos requieren sesión; whitelist de extensiones y prevención de path traversal.
- Facturación calculada en el servidor (subtotal + IVA 15 %), auditoría persistente de eventos sensibles.

Análisis completo: [SEGURIDAD_ISO27001_27010.md](SEGURIDAD_ISO27001_27010.md) · Desarrollo: [DOCUMENTACION_DESARROLLO.md](DOCUMENTACION_DESARROLLO.md)

