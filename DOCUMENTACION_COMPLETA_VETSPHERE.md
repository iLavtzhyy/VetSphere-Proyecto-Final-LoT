# VetSphere — documentación integral del proyecto

**Revisión:** 31 de octubre de 2026  
**Ámbito:** aplicación veterinaria VetSphere y módulo adicional de cadena de frío IoT.

## 1. Resumen

VetSphere es un sistema web para la operación de una clínica veterinaria. Reúne atención clínica, administración, farmacia, inventario, clientes y mascotas, agenda, facturación y reportes. La aplicación principal conserva su arquitectura Spring Boot + Angular. El nuevo monitoreo de cadena de frío se integra como un servicio Python/FastAPI independiente, conectado a la misma base SQL Server.

El módulo IoT se puede demostrar sin hardware: genera lecturas dinámicas de neveras y sensores simulados, guarda mediciones y alertas en SQL Server y actualiza el panel en tiempo real por WebSocket. La interfaz identifica claramente el origen simulado.

## 2. Tecnologías

| Capa | Tecnología | Uso |
|---|---|---|
| Frontend | Angular 17, TypeScript, RxJS, Angular Router | Aplicación SPA, formularios, control de roles y vistas |
| Interfaz | Tailwind CSS y estilos propios del proyecto | Diseño adaptable y coherente con VetSphere |
| Backend principal | Java 17, Spring Boot 3.3.5, Maven | API REST, reglas de negocio, servicios clínicos |
| Persistencia principal | Spring Data JPA, Hibernate, SQL Server | Datos veterinarios y administrativos |
| Seguridad | Spring Security, JWT HS256, BCrypt, Jakarta Validation | Sesiones sin estado, autorización y validación |
| Servicio SOAP | Apache CXF / JAX-WS, WS-Security UsernameToken PasswordDigest | Integración de inventario y servicio de demostración |
| Cadena de frío | Python 3, FastAPI, Uvicorn, Pydantic | API del módulo, validación, controles del simulador |
| Acceso SQL del servicio IoT | pyodbc, Microsoft ODBC Driver 18 | Guardado y consulta en la misma base SQL Server |
| Actualización en vivo | WebSocket | Lecturas, alertas y cambios de simulación en vivo |
| Exportación | Apache POI / OpenPDF en la aplicación existente; openpyxl / ReportLab en IoT | Exportaciones Excel y PDF |
| Mini servicio SOAP | Spring Boot / Java | Servicio auxiliar de demostración en puerto 8090 |
| Ejecución local | PowerShell, Docker Compose, Nginx | Arranque coordinado, contenedores y proxy WebSocket |
| Calidad | JUnit 5, Mockito, H2, JaCoCo, Jasmine/Karma | Pruebas del backend y frontend |

El requerimiento adjunto proponía Angular Material, pero el frontend actual está implementado con Tailwind y estilos propios y no incluye Angular Material. El módulo nuevo reutiliza el sistema de interfaz existente para evitar agregar una dependencia y mantener la misma apariencia.

## 3. Estructura del repositorio

```text
Backend-Proyecto-Lenguajes-main/Backend-Proyecto-Lenguajes-main/  Backend Spring Boot
Frontend-Proyecto-Lenguajes-main/Frontend-Proyecto-Lenguajes-main/ Aplicación Angular
MiniProyecto-SOAP-master/MiniProyecto-SOAP-master/               Servicio SOAP auxiliar
iot-cadena-frio/                                                   Servicio FastAPI del módulo IoT
docs/                                                              Guías y documentación existente
logs/                                                              Registros locales de ejecución
docker-compose.yml                                                 Ejecución en contenedores
iniciar-todo.ps1                                                   Arranque de todos los servicios locales
```

## 4. Módulos de VetSphere

La API principal organiza su dominio clínico y administrativo en controladores, servicios, repositorios, entidades y DTO. Los módulos implementados incluyen:

- Acceso, registro, verificación, recuperación de contraseña, perfil y permisos.
- Dashboard, métricas, reportes, configuración de clínica y auditoría.
- Clientes, usuarios, roles, veterinarios y especialidades.
- Mascotas, expedientes, archivos clínicos y seguimiento.
- Agenda, citas, turnos, recepción, flujo de admisión y triage.
- Consultas, vacunación, cirugías, hospitalización, urgencias e imagenología.
- Farmacia, productos, inventario, compras, proveedores y laboratorio.
- Caja, facturación, cupones, consentimientos e integración SOAP.
- Asistente conversacional y administración de historial del chat.
- Nuevo módulo de administrador **Cadena de frío IoT**.

La lista de rutas y permisos vive centralmente en `ReglasSeguridad`; la interfaz Angular usa guardas de rol alineadas con dichos permisos.

## 5. Cadena de frío IoT

### Funciones

- Administración de neveras, ubicación, capacidad y límites de temperatura.
- Gestión de sensores simulados y dispositivos físicos futuros.
- Simulación automática con intervalos de 5 o 10 segundos y modos normal, advertencia y crítico.
- Controles para iniciar, detener y reiniciar la simulación; provocar una lectura crítica o desconectar/reconectar un sensor simulado.
- Lecturas dinámicas de temperatura/humedad persistidas en SQL Server.
- Alertas por umbrales, cambios bruscos y falta de comunicación.
- Estado del sensor físico pasa a desconectado si no reporta en más de 30 segundos.
- Gestión manual de alertas con nota de acción; normalizar temperatura no cierra el incidente.
- Asociación de productos existentes de Farmacia con una nevera. Una exposición crítica solicita revisión humana y no declara automáticamente un medicamento como dañado.
- Gráfica, filtros por nevera/fecha e historial, exportación PDF y Excel.
- Actualizaciones en vivo vía WebSocket.
- Las mediciones y sensores muestran si son **SIMULADOS** o vienen de un **DISPOSITIVO**.

### Separación para futura integración física

La simulación usa su propio motor y escribe lecturas con origen `SIMULATED`. La recepción de dispositivos es el endpoint `POST /api/iot/readings`, requiere credencial individual del sensor y guarda el origen `DEVICE`. Los tokens físicos se generan al registrar un sensor, solo se muestran una vez, y en SQL Server se almacena únicamente su hash SHA-256. Un dispositivo simulado no puede enviar por la ruta física ni viceversa.

### Tablas creadas por el servicio

Al iniciar, el servicio crea las tablas si aún no existen y conserva los registros previos:

- `dbo.iot_refrigerators`
- `dbo.iot_sensors`
- `dbo.iot_temperature_readings`
- `dbo.iot_alerts`
- `dbo.iot_alert_actions`
- `dbo.iot_product_storage`

Las lecturas de demostración iniciales se insertan solo cuando no existen neveras de ejemplo con esos códigos. La inicialización no borra tablas ni datos existentes.

### API del módulo

Base local: `http://localhost:8091/api/iot`. Las rutas `/admin/**` requieren un JWT vigente de VetSphere con el rol `ROLE_ADMIN`.

| Método | Ruta | Propósito |
|---|---|---|
| GET | `/health` | Salud del servicio y conexión SQL |
| GET | `/admin/dashboard` | Indicadores y estado de neveras |
| GET/POST | `/admin/refrigerators` | Consultar y registrar neveras |
| PATCH/DELETE | `/admin/refrigerators/{id}` | Editar o desactivar conservando historial |
| GET/POST | `/admin/sensors` | Consultar y crear sensores físicos |
| DELETE | `/admin/sensors/{id}` | Desactivar sensor conservando trazabilidad |
| POST | `/admin/sensors/{id}/rotate-key` | Renovar la credencial física |
| POST | `/api/iot/readings` | Recibir medición de sensor con token individual |
| GET | `/admin/readings` | Consultar historial con filtros |
| GET | `/admin/readings/export` | Exportar historial en PDF o Excel |
| GET | `/admin/alerts` | Consultar alertas |
| POST | `/admin/alerts/{id}/actions` | Registrar atención o resolver con nota |
| GET | `/admin/simulation` | Estado del simulador |
| POST | `/admin/simulation/start` | Iniciar con modo/intervalo |
| POST | `/admin/simulation/stop` | Detenerlo preservando el historial |
| POST | `/admin/simulation/restart` | Reiniciarlo y reconectar los sensores de demo |
| POST | `/admin/simulation/force-critical/{id}` | Forzar temperatura fuera de rango |
| POST | `/admin/simulation/sensors/{id}/disconnect` | Simular pérdida de comunicación |
| POST | `/admin/simulation/sensors/{id}/reconnect` | Simular reconexión |
| WS | `/api/iot/ws?token=...` | Eventos en vivo, requiere JWT de administrador |

### Ejecución local de la simulación

Desde el panel de administrador, abre **Cadena de frío IoT**, selecciona intervalo y modo y pulsa **Iniciar**. El servicio queda activo en `http://localhost:8091`; la simulación se controla desde el panel y queda detenida cuando el servicio se reinicia. Los registros permanecen en SQL Server.

## 6. Roles, seguridad y datos

Roles principales: `ROLE_ADMIN`, `ROLE_VETERINARIO`, `ROLE_RECEPCIONISTA` y `ROLE_CLIENTE`. Las rutas IoT y la página Angular `/cadena-frio` son solo para administrador. El servicio verifica firma/expiración JWT con el mismo secreto configurado por el backend y revalida en SQL Server que la cuenta esté activa, no bloqueada y conserve el rol `ROLE_ADMIN`. El WebSocket recibe la credencial en los subprotocolos del handshake, no en la URL. La API física exige un token por sensor y valida que no sea un token del simulador.

Configuración sensible se lee de `.env` local; no se deben guardar secretos en este documento o en el repositorio. Variables del módulo: `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET`, `CORS_ALLOWED_ORIGINS`, `IOT_ODBC_DRIVER` y `IOT_LOG_LEVEL`. En Docker Compose, el servicio usa las mismas variables SQL/JWT y la red interna.

## 7. Puertos y arranque

| Servicio | Puerto local |
|---|---:|
| Angular | 4200 |
| API Spring Boot | 8080 |
| Mini SOAP | 8090 |
| API FastAPI IoT | 8091 |
| SQL Server Express | 1433 |

Requisitos: Windows, JDK 17+, Node/npm, Python 3.10+, Microsoft ODBC Driver 18 para SQL Server, SQL Server Express y variables de entorno completas.

```powershell
# Iniciar SQL Server y todos los servicios que aún no estén en línea
.\iniciar-todo.ps1

# Solo servicio IoT
cd .\iot-cadena-frio
.\iniciar-cadena-frio.ps1

# Compilar Angular
cd ..\Frontend-Proyecto-Lenguajes-main\Frontend-Proyecto-Lenguajes-main
npm run build

# Verificar backend y cobertura
cd ..\..\Backend-Proyecto-Lenguajes-main\Backend-Proyecto-Lenguajes-main
mvn verify
```

Con Docker, `docker compose up --build` agrega el contenedor `cold-chain-iot` y Nginx proxifica `/iot/` al puerto interno 8091, incluyendo cabeceras para WebSocket.

## 8. Pruebas y verificación de esta revisión

Resultados observados durante la revisión del 31-10-2026:

- Inicialización IoT en SQL Server: **correcta**; conectó a `VeterinariaDB` y ejecutó la creación idempotente de esquema.
- `python -m compileall` e importación de la app FastAPI: **correctas**; 32 rutas registradas.
- `mvn verify`: **correcto**, 146 pruebas aprobadas, 0 errores/fallos, cobertura JaCoCo mínima alcanzada. Se usó el Maven integrado de IntelliJ porque `mvn` no está en PATH.
- `npm test`: **correcto**, 414 pruebas aprobadas en Microsoft Edge 156. Se configuró temporalmente `CHROME_BIN` porque Chrome no está instalado en la ruta predeterminada. Dos specs antiguos no contienen expectativas; la suite termina con éxito.
- `npm run build`: **correcto**. Advertencias de tamaño CSS en Farmacia/Mascotas y paquete CommonJS `qrcode` en Facturación.
- FastAPI/SQL: **correcto**, conexión `VeterinariaDB`, esquema inicializado y API con autorización probada. Respondieron correctamente dashboard, neveras, sensores, alertas, lecturas, estadísticas, inventario asociado y estado del simulador.
- Simulador: **correcto**, se inició, persistió lecturas variables marcadas `SIMULATED` y se detuvo; el historial permaneció en SQL Server.
- Reglas y exportación: validación de temperatura normal/advertencia/crítica correcta; exportación XLSX y PDF produjo archivos válidos con lecturas guardadas.
- WebSocket: el JWT viaja en subprotocolos y no en la URL. Comparte la revalidación de usuario/rol del API.
- Estado de acceso existente: las cuentas ADMIN de esta base están temporalmente bloqueadas por la regla de intentos fallidos. La verificación recibió 401 para la cuenta bloqueada, 403 para un cliente y 401 sin sesión. No se modificaron usuarios ni se eludió el bloqueo; un administrador podrá ingresar cuando se desbloquee la cuenta.
- Arranque coordinado: backend, frontend, Mini SOAP, SQL Server y FastAPI en puerto 8091 respondieron como listos. URLs locales devuelven HTTP 200.

## 9. Límites y siguientes pasos

- Las lecturas del panel de demo son simuladas; para operación real deben instalarse sensores calibrados y conectarlos al endpoint físico mediante TLS, credencial individual y política de rotación.
- Antes de usar el módulo en producción, añadir pruebas de integración de las rutas IoT, límites/retención de datos, supervisión de disponibilidad y notificaciones operativas fuera del panel.
- Los medicamentos marcados requieren verificación del equipo farmacéutico/veterinario y registro de la decisión según el protocolo de conservación.
- Angular Material no se agregó porque el proyecto actual no lo utiliza; el diseño mantiene los componentes y estilos VetSphere existentes.

