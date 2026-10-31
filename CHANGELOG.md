# Changelog

Todo lo notable de VetSphere se documenta aquí. El formato sigue las convenciones de [Keep a Changelog](https://keepachangelog.com/es-1.1.0/) y el proyecto adota [Semantic Versioning](https://semver.org/lang/es/).

---

## [2.1.0] — 2026-10-21

Versión "farmacia comercial + cuentas seguras": medición real de la rotación de inventario, cupones de descuento calculados en el servidor, control del ciclo de vida de las cuentas y factura imprimible en PDF completo.

### ✨ Nuevo

- **Rotación de inventario medible** (`GET /api/farmacia/rotacion`): a partir de las ventas reales por producto (unidades, ingresos, última venta, cobertura en días y *capital dormido*) clasifica cada producto como `SIN_ROTACION`, `BAJA_ROTACION` o `ROTACION_SALUDABLE` y propone un descuento sugerido.
- **Cupones de descuento** (nuevo dominio `Cupon`/`CanjeCupon`): tipo `PORCENTAJE` o `VALOR_FIJO`, alcance por producto, por categoría o global, fecha de vigencia, compra mínima, límite de usos y origen (`MANUAL` o `BAJA_ROTACION`). CRUD completo, activación/desactivación y trazabilidad de cada canje.
- **Generación automática de promociones** (`POST /api/farmacia/cupones/auto`): crea cupones para los productos de baja rotación que aún no tienen promoción, saltándose los que no tienen existencias.
- **Simulación de cupón en el carrito** (`POST /api/farmacia/cupones/validar`): devuelve el importe que se cobrará (bruto, descuento, subtotal, IVA y total) usando exactamente el mismo cálculo que la factura, sin persistir nada.
- **Detalle de factura persistido** (entidad `FacturaDetalle` con FK a `Producto`): sin esto no había forma de saber qué se vendía; es la base de la rotación.
- **Descuento permanente por producto** (`Producto.descuento`), aplicado a nivel de línea y visible en el inventario.
- **Factura en PDF completa**: cabecera clínica configurable, emisor y receptor, tabla de ítems con descuento, totales con IVA, cupón aplicado, total en letras, observaciones, código de verificación, términos y condiciones, firma, pie con paginación y marca de agua `ANULADO` en facturas anuladas.
- **Ciclo de vida de las cuentas**: bloqueo automático tras 3 intentos fallidos (3 horas, HTTP 423), bloqueo/desbloqueo/desactivación/reactivación por el administrador, baja y reactivación del propio titular con verificación por correo y pantalla **“Mi cuenta”** (perfil, cambio de contraseña, baja y reactivación en dos pasos).
- **Numeración de facturas en el servidor**: `FAC-<año>-<6 dígitos>`. El frontend ya no inventa números.
- **Motivo de bloqueo y de baja** auditado en cada cambio de estado de una cuenta.

### 🔒 Seguridad

- **El precio del producto lo pone siempre el servidor**: si la línea de la factura lleva `productoCodigo`, el importe enviado por el cliente se ignora y se usa el precio del catálogo.
- **El cupón se valida y se calcula en el servidor**: alcance, vigencia, límite de usos, compra mínima y tope del descuento (el valor fijo nunca puede superar el subtotal ni dejar la factura en cero).
- **Mensajes de login neutros**: un correo no registrado y una contraseña incorrecta devuelven el mismo texto, para no permitir enumerar cuentas.
- **Códigos de verificación tipados y de un solo uso** (`VERIFICACION_CUENTA`, `RECUPERACION`, `DESACTIVACION`, `REACTIVACION`), con caducidad de 10 minutos y máximo 5 códigos por hora y correo.
- **`JwtAuthenticationFilter` valida en cada petición** que la cuenta siga activa y no esté bloqueada: bloquear o desactivar a un usuario invalida de inmediato sus tokens ya emitidos.
- **Stock y factura en la misma transacción**: si la factura falla, el descuento de stock se revierte con ella.
- Validación en DTOs, controladores y roles; reglas de Spring Security por método HTTP (los endpoints de cupones y rotación van antes que la regla general de farmacia porque en Spring Security gana la primera coincidencia).
- El cupón de valor fijo se limita al subtotal: nunca genera importes negativos.

### 🐛 Corregido

- El IVA se mostraba siempre como `$0.00` en la pantalla de facturación.
- Las facturas se numeraban en el frontend (`facturas.length + 1`), con riesgo de duplicados.
- El descuento permanente de un producto no se aplicaba al calcular la factura.
- `stockCritico()` solo tenía en cuenta `prioridadAlta`, por lo que no cuadraba con el contador del panel de farmacia.
- `CuponRepository.countByCuponId` era una consulta JPA inválida que impedía arrancar el contexto de Spring (fallaba la suite de integración completa).
- Un cupón de valor fijo igual al subtotal dejaba la factura a cero: ahora se rechaza con un error claro.

### 🎨 Frontend

- **Farmacia reorganizada en tres pestañas**: Inventario, Baja rotación (con filtros por estado y generación automática de promociones) y Cupones (CRUD con vigencia, alcance y límite de usos).
- El modal de venta de farmacia admite **cupón de descuento** con simulación previa del importe y recordatorio de que el total definitivo lo confirma el servidor.
- **Pantalla de usuarios**: bloquear, desbloquear, desactivar y reactivar cuentas (solo ADMIN), con motivo obligatorio y estado visible en la tabla (activo, desactivado, bloqueado, correo sin verificar, intentos fallidos).
- **Aviso específico de cuenta bloqueada (HTTP 423)** en el login, con el tiempo restante y la política aplicada.
- **Pantalla “Mi cuenta”** con perfil, cambio de contraseña con confirmación y baja/reactivación de la cuenta con verificación por correo.

### 🧪 Pruebas

- `AuthServiceTest` (nuevo): bloqueo al tercer intento, mensajes neutros, cuenta desactivada, códigos de un solo uso y códigos expirados.
- `FacturacionServiceTest`: casos nuevos de cupón porcentual, cupón de valor fijo, cupón que agota el subtotal, cupón no aplicable y stock insuficiente.
- `PromocionesFarmaciaIntegracionTest` (nuevo): análisis de rotación, permisos por rol, CRUD de cupones, simulación y factura con descuento registrado.
- `CuentasSeguridadIntegracionTest` (nuevo): bloqueo con HTTP 423, desbloqueo por el administrador, baja y reactivación del titular con verificación por correo, invalidación del token al desactivar y estado del perfil.

### 📄 Documentación

- README raíz actualizado con los nuevos módulos (rotación de inventario, cupones, control de cuentas y PDF de factura).

---

## [2.2.0] — 2026-10-21

Versión "datos reales": los módulos que devolvían información inventada pasan a leerse y escribirse en la base de datos, se elimina el *mass assignment* en los controladores y se endurece la autorización por rol.

### ✨ Nuevo

- **Módulos clínicos y administrativos con persistencia real** (dejan de ser "fachada" y ahora tienen CRUD completo sobre JPA): **vacunaciones** (calendario), **seguimientos postoperatorios**, **urgencias** (panel de activas), **turnos del personal** (sin solapamientos), **órdenes de compra** (correlativo `OC-<año>-<%04d>`), **imagenología** (estudios) y **consentimientos** (correlativo `CONS-<año>-<%04d>`, el quirúrgico ya se guarda en base de datos).
- **Triage veterinario por reglas** (`POST /api/triage/clasificar`): clasifica en Rojo/Naranja/Amarillo/Verde a partir de signos y constantes (vía aérea, consciencia, circulación, temperatura, frecuencia cardíaca y respiratoria, dolor), explica los motivos y propone el siguiente paso. Antes devolvía un nivel fijo salvo las convulsiones.
- **Cierre de caja real** (`GET /api/caja/cierre`): los importes se calculan con las facturas emitidas del día (pagadas, pendientes y anuladas); se elimina la cifra fija `1280.75`.
- **Configuración de la clínica** (`GET /api/configuracion-clinica`) leída de `application.properties` (`app.clinica.*`, `app.moneda`), con variables de entorno por instalación.
- **Ruta pública `/reactivar-cuenta`** en el frontend: una cuenta desactivada no puede iniciar sesión, así que la reactivación se hace sin sesión, en dos pasos (solicitar código → confirmar).
- **Matriz de permisos por rol** (`GET /api/permisos`) alineada con las reglas reales de Spring Security y verificada por prueba automática.
- **Una única fuente de verdad de permisos** (`ReglasSeguridad`): una tabla de reglas (módulo · modo · roles · métodos · patrones) construye a la vez la cadena de Spring Security y la matriz que ve el frontend. Antes cada una estaba escrita por separado y ya se habían desincronizado; ahora la divergencia es imposible por construcción.
- **Corrección de la numeración de facturas**: `FAC-<año>-<6 dígitos>` generado siempre en el servidor.
- **Datos de ejemplo de los módulos nuevos** en la semilla (`VetSphereDataSeeder`) y en `data.sql`: turnos, órdenes de compra, proveedores, vacunación, seguimientos, urgencias, estudios de imagen y consentimientos.

### 🔒 Seguridad

- **Fin del *mass assignment***: las citas, la ficha del veterinario, las consultas, las órdenes de laboratorio, las hospitalizaciones y los productos ya no reciben la entidad JPA cruda. Ahora usan DTOs validados (`CitaRequest`, `VeterinarioRequest`) y **el identificador lo asigna el servidor**: un alta nunca puede sobrescribir un registro existente.
- **Ficha pública del equipo** (`GET /api/veterinarios/publico`): antes devolvía la entidad `Usuario` completa en un endpoint abierto al público, lo que filtraba el estado de seguridad de las cuentas (intentos fallidos, bloqueo y su motivo, fecha de desactivación). Ahora solo se publica la cartelera.
- **Autorización de la ficha del veterinario**: `PUT /api/equipo-veterinario/{id}` no estaba cubierto por ninguna regla y quedaba abierto a cualquier usuario autenticado (incluido CLIENTE). Ahora solo el ADMIN lo modifica.
- **Estados de cita validados**: un estado inventado se rechaza con HTTP 400 y la lista de estados válidos se expone en `GET /api/citas/estados`.
- **Regla duplicada de `equipo-veterinario` eliminada**: la regla de escritura (solo ADMIN) estaba después de una regla general que la dejaba únicamente al ADMIN, dejando el GET del equipo restringido a un solo rol. Ahora la lectura es de ADMIN, VETERINARIO y RECEPCIONISTA, y la escritura sigue siendo exclusiva del ADMIN.
- **`GET /api/veterinarios` ya no filtra el estado interno de las cuentas**: devuelve el DTO `VeterinarioPublico` (id, nombres, apellidos, especialidad, cédula profesional, teléfono, correo, foto y biografía) igual que el endpoint público, y se abre al CLIENTE para que pueda ver la cartelera sin exponerse.
- **Rutas del frontend por rol**: `strictTemplates` activado en Angular, de modo que la compilación detecta plantillas inconsistentes con los tipos del componente.

### 🐛 Corregido

- **XSS almacenado en el historial imprimible**: `reportes.ts` interpolaba motivo, diagnóstico, tratamiento, procedimientos y resultados de laboratorio directamente en el HTML; ahora todo se escapa antes de insertarse (igual que ya se hacía en el comprobante de factura).
- **Rendimiento de la sesión por inactividad**: los oyentes de actividad se registraban dentro del ciclo de detección de cambios de Angular, por lo que *cada movimiento del ratón* disparaba una comprobación en toda la aplicación. Ahora viven fuera de la zona de Angular.
- La cuenta desactivada informaba de un estado ambiguo; ahora el mensaje indica que se puede reactivar con el código enviado al correo.
- La matriz de permisos que veía el frontend contradecía a `SecurityConfig` (por ejemplo, `RECEPCIONISTA` podía consultar la rotación y los cupones, pero no aparecían en la lista).
- **Contador de solicitudes pendientes** (`administracion.ts`): el `|| 1` sustituía un contador en cero por 1 antes de restar, así que tras una aprobación el indicador quedaba en 0 aunque quedaran solicitudes, y nunca se refrescaba desde el servidor.
- **Aviso de compilación NG8107** en `administracion.html`: el acceso opcional `u.roles[0]?.nombre` era innecesario para el tipo y, además, no contemplaba al usuario sin rol. Ahora la plantilla decide explícitamente y muestra "Sin rol".
- **Validación en cliente del registro y de la verificación por código**: `registro.ts` enviaba la petición aunque faltaran nombres, el correo no tuviera formato válido o la contraseña fuera de un solo carácter; `verificar-codigo.ts` enviaba códigos vacíos o de longitud arbitraria y pedía reenviar sin correo en sesión. Ahora ambos validan antes de llamar a la API (el servidor sigue validando: es defensa en profundidad).

### ✅ Pruebas

**Backend: 126 pruebas, 0 fallos.**

- `PermisoControllerTest` (nuevo): la matriz por rol coincide con las reglas de seguridad reales, y cada módulo de la tabla de reglas aparece en ella.
- `TriageServiceTest` (nuevo): cada nivel de triage con sus motivos y la normalización de las llaves.
- `ModulosClinicosIntegracionTest` (nuevo, 21 pruebas): los siete módulos convertidos de fachada a persistencia real (vacunación, seguimientos, urgencias, turnos, compras, imagenología y consentimientos) — alta, consulta, actualización, borrado, reglas de negocio (mascota inexistente, estado inválido, solapamiento de turnos, número de orden repetido, total negativo) y permisos 401/403 por rol.
- `SeguridadIntegracionTest`: la matriz de permisos solo la consulta el ADMIN; el RECEPCIONISTA consulta la rotación y los cupones pero no los administra.

**Frontend: 391 pruebas, 0 fallos.** Specs reales (comportamiento, datos, validaciones y permisos por rol; nada de *placeholders*):

- Núcleo: `app` (tema aplicado e inicio del control de inactividad), `icono` (glifos y fallback), `inicio` (secciones, servicios, pasos, clientes, equipo y enlace de WhatsApp codificado), `documentacion` (nueve criterios de ISO/IEC 25010 e impresión) y `barra-navegacion-admin` (filtrado de grupos por rol).
- Clínica: `agendar-cita`, `consultas`, `cirugias`, `laboratorio`, `hospitalizacion`, `mascotas`, `clientes`, `veterinarios`.
- Operación: `farmacia`, `facturacion`, `usuarios`, `administracion`, `reportes`, `registro`, `verificar-codigo` y `reactivar-cuenta`.

### 📄 Documentación

- README actualizado con los módulos nuevos, la matriz de roles derivada de `ReglasSeguridad` y el bloque de seguridad.
- `data.sql` actualizado con las tablas `factura_detalle`, `cupon`, `canje_cupon`, `turno`, `orden_compra`, `proveedor`, `vacunacion`, `seguimiento`, `urgencia`, `estudio_imagen` y `consentimiento` (semilla de referencia para SQL Server).

---

## [2.0.0] — 2026-10-17

Versión de "titulación": endurecimiento de seguridad, verificabilidad real, refactor de calidad y asistente de IA.

### ✨ Nuevo

- **Asistente de IA (`POST /api/chat`)** con function calling sobre datos reales del sistema (dashboard, mascotas, stock crítico, citas, facturación, usuario actual). Compatible con OpenAI, DeepSeek, Groq, LM Studio, etc. mediante `CHAT_API_KEY`, `CHAT_BASE_URL` y `CHAT_MODEL`. Widget flotante en el frontend para todas las pantallas autenticadas.
- **Documentación OpenAPI (Swagger UI)** en `/swagger-ui.html` (rol ADMIN), con esquema de seguridad JWT documentado.
- **Auditoría real persistente**: entidad `AuditorEvento` que registra logins, modificaciones de stock, facturas emitidas y eliminaciones críticas; el endpoint `/api/auditoria/eventos` ahora lee de la base de datos (antes devolvía datos inventados en memoria).
- **Proveedores e historial de inventario reales**: entidad `Proveedor` con CRUD y entidad `MovimientoInventario` (ENTRADA/SALIDA/AJUSTE con stock anterior/posterior y usuario) que sustituye los datos hardcodeados.
- **Tests de backend**: `spring-boot-starter-test` (JUnit 5 + Mockito + MockMvc) con H2 en memoria: unitarios de servicios (JWT, facturación, inventario, tarifas, chat) e integración de controladores con autorización por roles. Cobertura medida con **JaCoCo**.
- **Tests reales de frontend**: suites para `token.interceptor`, `autorizacion.service`, `auth.guard`, `api.service`, `theme.service` y el chatbot, ejecutables con Edge headless (`CHROME_BIN`).
- **CI/CD**: workflow de GitHub Actions que compila y ejecuta las pruebas de backend y frontend en cada push.
- **Notificaciones toast** en el frontend (`NotificacionService`) con `aria-live`, reemplazando los `alert()` nativos.
- **Indicadores de carga** en todas las pantallas con datos remotos.
- Guías técnicas: `docs/INSTALACION.md` y `docs/ARQUITECTURA.md` (diagramas Mermaid de componentes, datos, JWT, SOAP e IA).

### 🔒 Seguridad

- Filtro JWT: tokens inválidos o expirados ya no producen error 500; se responde 401 con el envoltorio estándar mediante *entry point* propio.
- `/uploads/**` dejó de ser público: descargar archivos clínicos requiere sesión autenticada.
- **Facturación server-side**: el backend recalcula subtotal, IVA (12 %) e total; ignora los importes enviados por el cliente (antes se persistían sin validar → manipulación de precios).
- Eliminada la contraseña por defecto `"cliente123"` al crear clientes: se genera una clave temporal aleatoria.
- Los módulos de fachada (auditoría, caja, compras, turnos, urgencias, vacunación, seguimientos, imagenología, especialidades, configuración clínica, consentimientos, triage, notificaciones, inventario) quedaron restringidos al rol ADMIN.
- Endurecimiento del endpoint de notificaciones (antes permitía enviar correos arbitrarios).
- Validación de entrada con `jakarta.validation` en los DTOs principales.
- `test_soap.py` ya no contiene credenciales en el repositorio: lee `SOAP_USERNAME`/`SOAP_PASSWORD` del entorno y valida la respuesta con aserciones.

### 🐛 Corregido

- Namespace del esquema SOAP `productos.xsd` alineado con el contrato (`https://soap.vetsphere.com/productos`) y agregadas las operaciones faltantes.
- Sobre SOAP 1.1 de los XML de prueba: namespace correcto (`http://schemas.xmlsoap.org/soap/envelope/`).
- `GlobalExceptionHandler`: manejo completo de validaciones (400 con detalle de campos), tipos inválidos (400), método no soportado (405) y error genérico (500) sin filtrar stacktraces.
- `App.code-workspace` apuntaba a una carpeta inexistente (`../App`); ahora referencia los tres servicios reales.
- Placeholder de contraseña que sugería una clave débil en la gestión de usuarios.

### ♻️ Mejoras de mantenibilidad

- Lógica de negocio movida de controladores a servicios (`FacturacionService`, entre otros) con `@Transactional` en operaciones multi-guardado.
- Precios hardcodeados en 4+ lugares reemplazados por `TarifaService` configurable por properties (`app.tarifas.*`).
- Eliminada la duplicación de `normalizarRol()` entre `SecurityConfig` y `JwtAuthenticationFilter`.
- Entornos de Angular (`environment.ts` / `environment.prod.ts`): la URL del API ya no está hardcodeada en el código; build de producción con optimización activada.
- CSS duplicado entre pantallas (facturación/reportes eran 92 % idénticos) consolidado en estilos globales compartidos.
- Paginación opcional (`?page=&size=`) en listados principales, compatible con la respuesta por defecto.
- Dependencia `lucide-angular` sin uso eliminada; `.gitignore` del repositorio ampliado (IDEs, logs, artefactos).

### 📄 Documentación

- README raíz reescrito (visión completa del ecosistema, roles, tecnologías, verificación).
- README de backend y frontend reescritos con instrucciones de ejecución, pruebas y estructura.
- `.env.example` ampliado con variables de CORS y del asistente de IA.

---

## [1.0.0] — 2026 (versión inicial)

- Sistema completo REST (Spring Boot 3 + JWT + SQL Server) con 20 pantallas Angular: login con 2FA, mascotas e historial clínico, citas, consultas, cirugías, laboratorio, hospitalización, farmacia, facturación, reportes PDF/Excel y administración de usuarios.
- Servicio SOAP con WS-Security (UsernameToken PasswordDigest) y cliente de sucursal (Mini SOAP, puerto 8090) con transferencias y patrón de compensación.
- Informe de calidad ISO/IEC 25010 integrado en la aplicación y presentación de defensa.
