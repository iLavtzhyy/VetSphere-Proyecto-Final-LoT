# 🏗️ Arquitectura de VetSphere

VetSphere es un sistema de información clínica veterinaria con **arquitectura híbrida REST + SOAP**: una SPA Angular consume una API REST JSON, y la integración B2B con sucursales se hace por SOAP 1.1 con WS-Security.

---

## 1. Vista de componentes

```mermaid
flowchart LR
    subgraph Cliente
        N[🌐 Navegador<br/>Angular 17 · Tailwind]
    end

    subgraph Backend["Backend :8080 — Spring Boot 3"]
        GW[🔒 Filtros de seguridad<br/>JWT · CORS · Cabeceras]
        REST[📡 API REST /api/**<br/>92+ endpoints]
        SOAP[🧾 Endpoint SOAP /soap/productos<br/>Apache CXF + WSS4J]
        IA[🤖 Servicio de IA /api/chat<br/>Function calling]
        REP[📄 Reportes PDF/Excel<br/>OpenPDF · Apache POI]
        AUD[🗂️ Auditoría de eventos]
    end

    subgraph Externos
        LLM[( API compatible<br/>con OpenAI )]
    end

    subgraph Sucursales
        MINI[🏬 Mini SOAP Client :8090<br/>Inventario de sucursal]
    end

    DB[🗄️ SQL Server<br/>VeterinariaDB]
    DB2[🗄️ SQL Server<br/>VeterinariaMiniDB]
    MAIL[📧 SMTP<br/>códigos 2FA / recuperación]

    N -- "HTTPS · JSON (Bearer JWT)" --> GW
    GW --> REST --> DB
    GW --> SOAP --> DB
    GW --> IA -- "HTTPS" --> LLM
    REST --> REP
    REST --> AUD
    REST -.-> MAIL
    MINI -- "SOAP + WS-Security<br/>UsernameToken PasswordDigest" --> SOAP
    MINI --> DB2
```

## 2. Arquitectura por capas del backend

```mermaid
flowchart TB
    C[Controller<br/>38 controladores REST] --> S[Service<br/>lógica de negocio · @Transactional]
    S --> R[Repository<br/>Spring Data JPA]
    R --> DB2[(SQL Server)]
    C -. DTOs y validación .-> C
    S -. Auditoría .-> A[(AuditorEvento)]

    subgraph Transversal
        SEC[Security · JWT filter · Roles]
        EXC[GlobalExceptionHandler]
        CFG[Config · CORS · CXF · Uploads]
    end
```

Patrones aplicados:
- **Envoltorio único de respuesta:** todo endpoint responde `{ ok, mensaje, data }` (clase `ApiResponse<T>`), lo que unifica manejo de errores y éxitos en el cliente.
- **Autorización declarativa por URL** en `SecurityConfig` + verificación de propiedad (un CLIENTE solo ve sus mascotas).
- **Lógica de negocio en servicios transaccionales**; los controladores solo validan y delegan.
- **Precios configurables** (`TarifaService`) en lugar de constantes incrustadas.

## 3. Modelo de datos principal

```mermaid
erDiagram
    USUARIO ||--o{ MASCOTA : "es propietario de"
    ROL }o--o{ USUARIO : "tiene"
    MASCOTA ||--o{ HISTORIAL_CLINICO : "presenta"
    MASCOTA ||--o{ CIRUGIA : "se le practica"
    MASCOTA ||--o{ HOSPITALIZACION : "ingresa a"
    MASCOTA ||--o{ ORDEN_LABORATORIO : "tiene"
    MASCOTA ||--o{ CITA : "agendada en"
    USUARIO ||--o{ HISTORIAL_CLINICO : "atendida por (veterinario)"
    USUARIO ||--o{ CITA : "atiende (veterinario)"
    FACTURA ||--o{ HISTORIAL_CLINICO : "cubre"
    FACTURA ||--o{ CIRUGIA : "cubre"
    FACTURA ||--o{ ORDEN_LABORATORIO : "cubre"
    FACTURA ||--o{ HOSPITALIZACION : "cubre"
    FACTURA }o--|| USUARIO : "emitida a"
    FACTURA }o--|| MASCOTA : "paciente"
    PRODUCTO ||--o{ MOVIMIENTO_INVENTARIO : "genera"
    USUARIO ||--o{ AUDITOR_EVENTO : "origina"

    USUARIO { int id PK "email unique · password BCrypt" }
    ROL { string nombre PK "ADMIN VETERINARIO RECEPCIONISTA CLIENTE" }
    MASCOTA { int id PK "especie raza peso estado" }
    HISTORIAL_CLINICO { int id PK "diagnostico tratamiento signosVitales precio" }
    CIRUGIA { int id PK "procedimiento anestesia estado precio" }
    HOSPITALIZACION { int id PK "jaula planCuidados estado precio" }
    ORDEN_LABORATORIO { int id PK "tipoPrueba resultado estado precio" }
    CITA { int id PK "fechaHora motivo estado" }
    FACTURA { int id PK "numero subtotal impuestos total estadoPago" }
    PRODUCTO { string codigo PK "stock stockCritico precio" }
    MOVIMIENTO_INVENTARIO { int id PK "ENTRADA SALIDA AJUSTE cantidad" }
    AUDITOR_EVENTO { int id PK "accion entidad detalle fecha" }
```

Notas de integridad:
- La vinculación de servicios clínicos a facturas es una relación JPA (`facturaId` se expone en JSON por compatibilidad).
- El stock se modifica solo dentro de `@Transactional` con bloqueo pesimista, y cada cambio genera un `MovimientoInventario` con stock anterior/posterior.
- La auditoría es persistente (no en memoria): login, cambios de stock, facturas emitidas y eliminaciones críticas quedan registrados con usuario y fecha.

## 4. Flujo de autenticación (JWT + 2FA)

```mermaid
sequenceDiagram
    actor U as Usuario
    participant FE as Angular
    participant API as Backend /api
    participant DB as SQL Server
    participant M as SMTP

    U->>FE: Email + contraseña
    FE->>API: POST /api/auth/login
    API->>DB: Verifica BCrypt
    alt Cuenta pendiente de verificación
        API->>M: Envía código de 6 dígitos (expira 10 min)
        U->>FE: Ingresa el código
        FE->>API: POST /api/auth/verificar-2fa
        API->>DB: Marca cuenta activa
    end
    API-->>FE: { token JWT (30 min), nombre, roles }
    FE->>FE: Guarda token · agrega Authorization: Bearer
    loop Cada petición
        FE->>API: Request + Bearer token
        API->>DB: Revalida roles del usuario
        API-->>FE: ApiResponse { ok, mensaje, data }
    end
    Note over FE: Expiración por inactividad (30 min)<br/>y redirección a /login
```

## 5. Integración SOAP con WS-Security

```mermaid
sequenceDiagram
    participant S as Sucursal (Mini SOAP :8090)
    participant B as Backend /soap/productos
    participant DB as SQL Server

    Note over S: Construye envelope SOAP 1.1 a mano:<br/>nonce aleatorio + SHA-1 → PasswordDigest
    S->>B: Envelope + cabecera WS-Security<br/>(UsernameToken / PasswordDigest / Nonce / Created)
    B->>B: WSS4JInInterceptor valida digest<br/>(comparación en tiempo constante)
    B->>DB: Operación de inventario
    B-->>S: Response XML (consultar/agregar/descontar/stock)
    Note over S: Transferencias con patrón de<br/>compensación manual ante fallos
```

Operaciones expuestas: `consultarProducto`, `agregarStock`, `descontarStock`, `actualizarStock`, `stockCritico`. El WSDL se publica en `/soap/productos?wsdl`. En el frontend, la sección de Farmacia incluye un panel para probar la integración SOAP.

## 6. Asistente de IA (function calling)

```mermaid
sequenceDiagram
    actor U as Usuario
    participant FE as Widget chatbot
    participant API as POST /api/chat
    participant LLM as API compatible con OpenAI
    participant DB as Datos del sistema

    U->>FE: "¿qué productos tienen stock crítico?"
    FE->>API: { mensaje, historial (últimos 10) }
    API->>LLM: chat/completions + tools definidos
    LLM-->>API: tool_call: stock_critico()
    API->>DB: Ejecuta la herramienta con datos reales
    API->>LLM: tool result
    LLM-->>API: respuesta final en español
    API-->>FE: ApiResponse { respuesta, herramientas[] }
    FE-->>U: Burbuja de respuesta
```

Herramientas disponibles: `dashboard_resumen`, `buscar_mascotas`, `stock_critico`, `citas_pendientes`, `resumen_facturacion`, `informacion_usuario`. Máximo 3 iteraciones de herramientas por mensaje; sin `CHAT_API_KEY` el sistema responde sin errores.

## 7. Decisiones de diseño relevantes

| Decisión | Justificación |
|---|---|
| REST para el frontend, SOAP para la integración B2B | REST por simpleza en SPA; SOAP por contrato tipado e industria (WS-Security) en la integración entre sistemas |
| JWT stateless + revalidación de roles en cada petición | Los cambios de rol surten efecto inmediato, sin revocar tokens |
| Cálculo de totales **solo en el servidor** | Evita manipulación de precios por parte del cliente |
| Envoltorio `{ok, mensaje, data}` | Manejo uniforme de errores en el frontend |
| Auditoría persistente | Requisito de trazabilidad alineado a ISO/IEC 27001 |
| Precios en configuración (`TarifaService`) | Cambiar tarifas no requiere recompilar |
| Paginación opcional (`?page=&size=`) | Compatibilidad hacia atrás con clientes existentes |
| Perfil `local` con datos de demostración | Reproducibilidad de demos y pruebas sin exponer datos reales |

## 8. Alineación con normas

- **ISO/IEC 25010:2023** — el informe de calidad interno (`/calidad-software` en la app, rol ADMIN) evalúa las 8 características de calidad de producto; el backend mide cobertura con JaCoCo y el frontend con Karma/Jasmine.
- **ISO/IEC 27001/27010** — controles: BCrypt, JWT con expiración, WS-Security, endurecimiento XML (anti-XXE), validación de entrada, whitelist de archivos subidos, auditoría de eventos y manejo de secretos por variables de entorno (nunca en el repositorio).
