# 🩺 VetSphere — Sistema de Información Clínica Veterinaria

**Arquitectura híbrida REST + SOAP · Spring Boot 3 · Angular 17 · SQL Server**

VetSphere es un sistema integral para la gestión de una clínica veterinaria: pacientes con historial clínico digital, citas, consultas, cirugías, laboratorio, hospitalización, farmacia con inventario, facturación con IVA, panel administrativo, reportes PDF/Excel y una **integración B2B por SOAP con WS-Security** para la sincronización de inventario con sucursales. Incluye un **asistente de IA** (chatbot con function calling) que responde consultas operativas con datos reales del sistema.

---

## ✨ Funcionalidades

| Módulo | Descripción |
|---|---|
| 🔐 **Autenticación y autorización** | JWT (30 min), 4 roles (ADMIN, VETERINARIO, RECEPCIONISTA, CLIENTE), verificación por código de 6 dígitos (2FA), recuperación de contraseña por correo, aprobación de solicitudes de personal |
| 🛡️ **Control de cuentas** | Bloqueo automático tras 3 intentos fallidos (3 h, HTTP 423), bloqueo/desbloqueo/desactivación por el administrador, baja y reactivación del titular con verificación por correo y pantalla “Mi cuenta” |
| 🐾 **Pacientes (mascotas)** | Ficha completa, fotos, línea de tiempo clínica, adjuntos (radiografías, resultados), autorización por dueño |
| 📋 **Historial clínico** | Eventos con signos vitales, diagnóstico, tratamiento, vacunas y adjuntos; exportación PDF |
| 📅 **Citas** | Agenda por veterinario, estados validados, panel de recepción |
| 🩹 **Consultas / Cirugías / Laboratorio / Hospitalización** | CRUD completo con precios configurables y vinculación a facturación |
| 💉 **Vacunación, seguimientos, urgencias e imagenología** | Calendario de vacunación, controles postoperatorios, panel de urgencias activas y estudios de imagenología, todo con persistencia real en base de datos |
| 🚑 **Triage veterinario** | Clasificación por reglas (Rojo/Naranja/Amarillo/Verde) a partir de signos y constantes, con los motivos y el siguiente paso |
| 📄 **Consentimientos y turnos** | Consentimientos quirúrgicos con folio correlativo (`CONS-<año>-<n>`) y turnos del personal sin solapamientos |
| 🧾 **Compras y proveedores** | Catálogo de proveedores y órdenes de compra con número correlativo `OC-<año>-<n>`, estados y responsable |
| 💊 **Farmacia e inventario** | Stock crítico, movimientos auditados (ENTRADA/SALIDA/AJUSTE/VENTA), venta con reverso de stock y descuento permanente por producto |
| 📉 **Rotación de inventario** | Medición real de ventas por producto (unidades, ingresos, última venta, cobertura en días, capital dormido) que detecta qué productos **no se venden** y propone un descuento |
| 🎟️ **Cupones de descuento** | Porcentaje o valor fijo, alcance por producto/categoría/global, vigencia, compra mínima, límite de usos, generación automática para baja rotación y simulación del importe antes de cobrar |
| 💳 **Facturación** | Cálculo **server-side** de subtotal, IVA 15 %, descuentos y total (no se confía en el cliente), estados de pago y **factura en PDF completa** (cabecera, emisor/receptor, ítems, totales, total en letras, términos, paginación) |
| 👥 **Clientes y personal** | CRUD, roles, aprobación/rechazo de solicitudes, equipo veterinario público |
| 📊 **Dashboard e indicadores** | KPIs en tiempo real, alertas de stock, citas del día, últimos 7 días |
| 📑 **Reportes** | PDF (OpenPDF) e Excel (Apache POI) |
| 🧾 **SOAP + WS-Security** | Endpoint `/soap/productos` (Apache CXF, UsernameToken PasswordDigest) + cliente sucursal (Mini SOAP) con transferencias compensadas |
| 🤖 **Asistente de IA** | `POST /api/chat`: chatbot con function calling sobre datos reales (dashboard, mascotas, stock, citas, facturación). Compatible con OpenAI/DeepSeek/Groq/LM Studio vía variables de entorno |
| 📈 **Auditoría real** | Bitácora de eventos (login, cambios de stock, facturas emitidas, mascotas eliminadas) persistida en BD |

## 🏗️ Estructura del repositorio

```
Calidad-De-Software-Vet/
├── Backend-Proyecto-Lenguajes-main/
│   └── Backend-Proyecto-Lenguajes-main/   → API REST + servicio SOAP (Spring Boot 3, Java 17)
├── Frontend-Proyecto-Lenguajes-main/
│   └── Frontend-Proyecto-Lenguajes-main/  → SPA Angular 17 + Tailwind (puerto 4200)
├── MiniProyecto-SOAP-master/
│   └── MiniProyecto-SOAP-master/          → Cliente SOAP "sucursal" (puerto 8090)
├── docs/                                  → Instalación y arquitectura
├── iniciar-todo.ps1                       → Levanta los 3 servicios en ventanas separadas
├── test_soap.py                           → Prueba manual del endpoint SOAP (WS-Security)
├── test_calidad_sistema.py                → Verificación de integridad estructural
├── reporte_calidad.html / .json           → Reporte de calidad ISO/IEC 25010
└── presentacion_vetsphere.html            → Presentación de defensa del proyecto
```

## 🚀 Inicio rápido

```powershell
# 1. Preparar la base de datos (SQL Server local)
.\Backend-Proyecto-Lenguajes-main\Backend-Proyecto-Lenguajes-main\scripts\Preparar-SqlServer.ps1

# 2. Configurar secretos (copiar .env.example → .env y completar)

# 3. Levantar todo (backend :8080 · frontend :4200 · mini SOAP :8090)
.\iniciar-todo.ps1
```

Guía completa paso a paso: **[docs/INSTALACION.md](docs/INSTALACION.md)** · Arquitectura y diagramas: **[docs/ARQUITECTURA.md](docs/ARQUITECTURA.md)**

## 🔑 Roles y permisos

> 🧭 **Una sola fuente de verdad.** La tabla de reglas de acceso vive en `ReglasSeguridad` (backend). Con ella se construye la cadena de Spring Security **y** se deriva la matriz que el frontend consume en `GET /api/permisos`. Antes ambas cosas estaban escritas por separado y ya se habían desincronizado; ahora la divergencia es imposible por construcción.

| Módulo / capacidad | ADMIN | VETERINARIO | RECEPCIONISTA | CLIENTE |
|---|:-:|:-:|:-:|:-:|
| `usuarios` — administrar cuentas, roles y solicitudes | ✅ | — | — | — |
| `permisos` — consultar la matriz de roles | ✅ | — | — | — |
| `auditoria`, `estadisticas`, `administracion`, `notificaciones` | ✅ | — | — | — |
| `documentacion-api` — Swagger / OpenAPI | ✅ | — | — | — |
| `integraciones` — proxy SOAP de sucursales | ✅ | — | — | — |
| `caja` — arqueo de caja | ✅ | — | — | — |
| `consultas`, `cirugias`, `laboratorio`, `hospitalizaciones` | ✅ | ✅ | — | — |
| `dashboard` — indicadores de la clínica | ✅ | ✅ | ✅ | — |
| `citas` — agenda y estados de cita | ✅ | ✅ | ✅ | — |
| `clientes`, `recepcion` — ficha de clientes y recepción | ✅ | — | ✅ | — |
| `facturacion` — emitir y consultar facturas | ✅ | — | ✅ | — |
| `farmacia`, `inventario` — stock y movimientos | ✅ | — | ✅ | — |
| `cupones` — consultar y simular descuentos | ✅ | — | ✅ | — |
| `cupones` — crear, editar y desactivar cupones | ✅ | — | — | — |
| `rotacion` — medir la rotación de inventario | ✅ | — | ✅ | — |
| `chat` — asistente de IA con datos reales | ✅ | ✅ | ✅ | — |
| `equipo-veterinario` — **leer** la ficha profesional | ✅ | ✅ | ✅ | — |
| `equipo-veterinario` — **editar** la ficha profesional | ✅ | — | — | — |
| `mascotas` — **leer** las fichas de pacientes | ✅ | ✅ | ✅ | 👀 (solo los suyos) |
| `mascotas` — **crear y editar** pacientes | ✅ | ✅ | ✅ | — |
| `veterinarios` — cartelera del equipo | ✅ | ✅ | ✅ | ✅ |
| `cuenta` — perfil y estado de la cuenta propia | ✅ | ✅ | ✅ | ✅ |
| `reportes-mascotas` — PDF del historial de una mascota | ✅ | ✅ | ✅ | ✅ |
| `reportes-finanzas` — Excel de finanzas | ✅ | — | ✅ | — |
| `reportes` — demás reportes y exportaciones | ✅ | — | — | — |
| `archivos` — adjuntos de la clínica | ✅ | ✅ | ✅ | ✅ |
| `turnos`, `compras`, `proveedores`, `urgencias`, `vacunacion`, `seguimientos`, `imagenologia`, `consentimientos`, `triage`, `especialidades`, `configuracion-clinica` | ✅ | — | — | — |

> 💡 El cálculo de todos los importes es **server-side**: el backend recalcula subtotal, descuentos, IVA y total, y aplica el cupón y el descuento de producto. El frontend solo muestra lo que devuelve el servidor.

> 🚪 Una cuenta desactivada no puede iniciar sesión: la **reactivación** se hace en la pantalla pública `/reactivar-cuenta`, en dos pasos (solicitar código → confirmarlo).

> 🧪 La matriz se verifica con `PermisoControllerTest`, que comprueba que lo que la interfaz promete es exactamente lo que el backend autoriza.

## 🎟️ Cómo funcionan los cupones de baja rotación

1. **Medir**: `GET /api/farmacia/rotacion` cruza las facturas emitidas con el inventario y clasifica cada producto en `SIN_ROTACION` (nunca vendido), `BAJA_ROTACION` (más de 30 días sin venderse) o `ROTACION_SALUDABLE`, además de calcular la cobertura en días y el capital dormido.
2. **Promover**: `POST /api/farmacia/cupones/auto` crea un cupón por cada producto de baja rotación (descuento sugerido según los días sin venta, con tope del 30 % y vigencia configurable).
3. **Simular**: `POST /api/farmacia/cupones/validar` devuelve el importe exacto que se cobrará, sin persistir nada, para mostrarlo en el carrito.
4. **Cobrar**: al emitir la factura, el backend vuelve a validar el cupón (vigencia, alcance, límite de usos y compra mínima), aplica el descuento, registra el canje y descuenta el stock en la misma transacción.

## 🛠️ Tecnologías

**Backend:** Spring Boot 3.3.5 · Java 17 · Spring Security (JWT) · Spring Data JPA · Apache CXF + WSS4J (SOAP) · SQL Server · Apache POI · OpenPDF · springdoc-openapi (Swagger UI) · JUnit 5 + Mockito + H2 (tests) · JaCoCo (cobertura)

**Frontend:** Angular 17 (standalone, TS strict) · Tailwind CSS (modo oscuro incluido) · RxJS · Karma/Jasmine

**Integración:** REST (JSON, envoltorio `{ok, mensaje, data}`) + SOAP 1.1 con WS-Security UsernameToken **PasswordDigest**

## 🤖 Asistente de IA

El backend expone `POST /api/chat` con *function calling* sobre datos reales del sistema (dashboard, mascotas por término de búsqueda, stock crítico, citas pendientes, resumen de facturación). Usa una API **compatible con OpenAI**, así que funciona con OpenAI, DeepSeek, Groq, LM Studio local, etc.:

```powershell
$env:CHAT_API_KEY="sk-..."
$env:CHAT_BASE_URL="https://api.openai.com/v1"   # opcional
$env:CHAT_MODEL="gpt-4o-mini"                    # opcional
```

Si no hay clave configurada, el asistente responde en la interfaz que no está habilitado (la app no falla). En el frontend el widget flotante aparece en la esquina inferior derecha de todas las pantallas autenticadas.

## ✅ Verificación y calidad

```powershell
# Tests de backend (JUnit 5 + H2 en memoria) + reporte de cobertura JaCoCo
$env:JAVA_HOME="C:\Program Files\JetBrains\IntelliJ IDEA <versión>\jbr"
& "C:\Program Files\JetBrains\IntelliJ IDEA <versión>\plugins\maven-plugin\lib\maven3\bin\mvn.cmd" -f .\Backend-Proyecto-Lenguajes-main\Backend-Proyecto-Lenguajes-main\pom.xml verify

# Tests de frontend (Karma/Jasmine con Edge headless)
$env:CHROME_BIN="C:\Program Files (x86)\Microsoft\Edge\Application\msedge.exe"
cd .\Frontend-Proyecto-Lenguajes-main\Frontend-Proyecto-Lenguajes-main
npm ci; npx ng test --watch=false --browsers=ChromeHeadless

# Documentación interactiva de la API (solo rol ADMIN autenticado)
# http://localhost:8080/swagger-ui.html
```

- **Normas de referencia:** ISO/IEC 25010:2023 (calidad de producto) e ISO/IEC 27001/27010 (seguridad/confianza). El informe de auditoría interna está disponible dentro de la app en **/calidad-software** (rol ADMIN) y en `reporte_calidad.html`.
- **Integridad estructural:** `python test_calidad_sistema.py` verifica que los tres servicios mantengan su estructura mínima.
- **Estado de la suite:** backend **126 pruebas** (JUnit 5 + H2 + MockMvc, 0 fallos) y frontend **391 pruebas** (Karma/Jasmine, 0 fallos). Los specs del frontend comprueban comportamiento real (peticiones, validaciones, estados y permisos por rol), no la mera instantiation del componente.

## 🔒 Seguridad

- Contraseñas con **BCrypt**, JWT firmado (HS256) con expiración de 30 min y revocación por inactividad en el frontend.
- Autorización **por URL y por rol** en el backend; archivos clínicos subidos requieren sesión.
- **Sin *mass assignment***: los controladores reciben DTOs validados (`CitaRequest`, `VeterinarioRequest`, `FacturaRequest`, `CuponRequest`…) y el identificador de cada registro lo asigna el servidor, de modo que un alta nunca sobrescribe un registro existente.
- **La cartelera pública del equipo** (`GET /api/veterinarios/publico`) solo expone los datos profesionales: no filtra ni el estado de seguridad de las cuentas (intentos fallidos, bloqueo ni su motivo).
- **La ficha del veterinario solo la modifica el ADMIN**: cada URL sensible tiene su regla explícita por rol y método HTTP, y la matriz que ve el frontend (`/api/permisos`) se verifica contra esa configuración.
- **Una única tabla de reglas** (`ReglasSeguridad`): con ella se construye la cadena de Spring Security y se deriva la matriz de permisos que ve el frontend, de modo que la interfaz no puede ofrecer una acción que el backend vaya a rechazar.
- **Cuentas bloqueables**: 3 intentos fallidos bloquean el acceso 3 horas (HTTP 423) y el titular recibe aviso por correo; el administrador puede bloquear, desbloquear, desactivar y reactivar, y el propio titular puede darse de baja y volver a activarse verificando su correo.
- El filtro JWT **revisa en cada petición** que la cuenta siga activa y no esté bloqueada: bloquear o desactivar invalida de inmediato los tokens ya emitidos.
- **Precios y descuentos los decide el servidor**: el precio de un producto, el descuento de línea y el cupón se calculan en el backend; el valor enviado por el cliente se ignora.
- **Códigos de verificación** de un solo uso, tipados, con caducidad de 10 minutos y límite de 5 por hora y correo; las respuestas de recuperación y de baja son neutras para no revelar qué correos existen.
- Endurecimiento XML (límites anti-XXE/entidades) y WS-Security PasswordDigest en el canal SOAP.
- Validación de entrada (jakarta.validation) y manejo centralizado de excepciones sin stacktraces.
- Auditoría de eventos sensibles en base de datos.

Ver `SEGURIDAD_ISO27001_27010.md` (carpeta del backend) para el análisis completo de controles y hallazgos.

## 📄 Documentación

| Documento | Contenido |
|---|---|
| [docs/INSTALACION.md](docs/INSTALACION.md) | Requisitos, base de datos, variables de entorno, ejecución, solución de problemas |
| [docs/ARQUITECTURA.md](docs/ARQUITECTURA.md) | Diagramas (componentes, modelo de datos, JWT, SOAP) y decisiones de diseño |
| [CHANGELOG.md](CHANGELOG.md) | Historial de versiones y mejoras |
| `Backend-.../DOCUMENTACION_DESARROLLO.md` | Detalle de la arquitectura del backend |
| `Frontend-.../DOCUMENTACION_VETSPHERE.md` | Detalle del frontend |
| `presentacion_vetsphere.html` | Presentación de defensa (abrir en navegador) |

## 👥 Créditos

Proyecto desarrollado como trabajo de la asignatura de Calidad de Software — arquitectura híbrida REST + SOAP con énfasis en seguridad, verificabilidad y estándares ISO.
