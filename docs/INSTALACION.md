# 🧭 Guía de instalación y puesta en marcha — VetSphere

Este documento explica cómo levantar el ecosistema completo de VetSphere desde cero en Windows.

---

## 1. Requisitos previos

| Componente | Versión mínima | Para qué |
|---|---|---|
| Windows | 10/11 | Entorno objetivo de los scripts `.ps1` |
| SQL Server | 2019 (Express, Developer o LocalDB) | Base de datos `VeterinariaDB` y `VeterinariaMiniDB` |
| JDK | 17 o superior | Backend y cliente SOAP (con el JBR de IntelliJ también funciona) |
| Maven | 3.9+ **o** IntelliJ IDEA (trae Maven embebido) | Compilación de los servicios Java |
| Node.js | 18+ (probado con 24) | Frontend Angular |
| Python | 3.x (opcional) | Scripts de verificación `test_*.py` |
| Cuenta SMTP (opcional) | Gmail u otro | Envío de códigos 2FA y recuperación de contraseña |
| Clave de API compatible con OpenAI (opcional) | — | Habilita el asistente de IA (`/api/chat`) |

> **Nota sobre Maven:** los scripts `iniciar-*.ps1` buscan primero el Maven embebido de IntelliJ IDEA (`plugins\maven-plugin\lib\maven3\bin\mvn.cmd`) y, si no existe, usan `mvn` del PATH. Si tienes Maven instalado aparte, todo funciona igual.

---

## 2. Base de datos

Ejecuta el script preparador (crea las dos bases de datos y el login de SQL Server):

```powershell
cd Backend-Proyecto-Lenguajes-main\Backend-Proyecto-Lenguajes-main
.\scripts\Preparar-SqlServer.ps1
```

Crea:
- `VeterinariaDB` — base del backend (usuarios, mascotas, clínica, facturación, inventario).
- `VeterinariaMiniDB` — base del cliente SOAP (inventario de la sucursal).

El esquema lo genera Hibernate con `spring.jpa.hibernate.ddl-auto=update` en el perfil `local`. Si prefieres credenciales distintas a las del script, edítalas antes de ejecutar.

---

## 3. Backend (Spring Boot, puerto 8080)

```powershell
cd Backend-Proyecto-Lenguajes-main\Backend-Proyecto-Lenguajes-main
Copy-Item .env.example .env
# Edita .env y completa los secretos (ver tabla de la sección 4)
.\iniciar-backend.ps1
```

El script carga automáticamente `.env` en las variables de entorno del proceso y ejecuta `mvn spring-boot:run`.

**Verificación:**
- `http://localhost:8080/api` responde (401/404 esperado sin token).
- `http://localhost:8080/swagger-ui.html` muestra la UI OpenAPI (requiere iniciar sesión como ADMIN y usar el botón *Authorize* con el JWT).
- `http://localhost:8080/soap/productos?wsdl` devuelve el WSDL del servicio SOAP.

---

## 4. Variables de entorno del backend

| Variable | Obligatoria | Descripción |
|---|---|---|
| `DB_URL` | ✅ | JDBC de SQL Server hacia `VeterinariaDB` |
| `DB_USERNAME` / `DB_PASSWORD` | ✅ | Credenciales de la base |
| `JWT_SECRET` | ✅ | Secreto HS256 de al menos 32 bytes |
| `SOAP_USERNAME` / `SOAP_PASSWORD` | ✅ | Credenciales WS-Security del endpoint SOAP |
| `ADMIN_REGISTRATION_CODE` | ✅ (para personal) | Código que el admin comparte al personal al registrarse |
| `MAIL_USERNAME` / `MAIL_PASSWORD` | Si usas 2FA/recuperación | SMTP transaccional |
| `DEMO_SEED_PASSWORD` | ❌ | Si se define, el perfil `local` siembra datos de demostración con esa contraseña para todos los usuarios demo |
| `CORS_ALLOWED_ORIGINS` | ❌ | Orígenes permitidos (default `http://localhost:4200`) |
| `CHAT_API_KEY` | ❌ | Clave del asistente de IA (OpenAI/DeepSeek/Groq/LM Studio) |
| `CHAT_BASE_URL` | ❌ | Default `https://api.openai.com/v1` |
| `CHAT_MODEL` | ❌ | Default `gpt-4o-mini` |

### Cuentas de demostración

Con `DEMO_SEED_PASSWORD` definido, al arrancar se crean estos usuarios (contraseña = el valor de la variable):

| Rol | Email |
|---|---|
| ADMIN | `admin1@vetsphere.com` |
| RECEPCIONISTA | `ana.molina@vetsphere.com` |
| VETERINARIO | `sofia.cardenas@vetsphere.com`, `mateo.andrade@vetsphere.com`, `lucia.rivera@vetsphere.com` |
| CLIENTE | `martin.alvarez@vetsphere.com`, `carolina.paredes@vetsphere.com`, `sebastian.cruz@vetsphere.com`, `fernanda.luna@vetsphere.com`, `oscar.reyes@vetsphere.com` |

---

## 5. Frontend (Angular, puerto 4200)

```powershell
cd Frontend-Proyecto-Lenguajes-main\Frontend-Proyecto-Lenguajes-main
npm install
npm start          # ó: .\iniciar-frontend.ps1
```

Abre `http://localhost:4200`. La URL del API se configura en `src/environments/environment.ts` (`apiUrl: 'http://localhost:8080/api'`); para despliegue propio, edita `environment.prod.ts` y compila con `npm run build`.

---

## 6. Cliente SOAP “sucursal” (puerto 8090)

```powershell
cd MiniProyecto-SOAP-master\MiniProyecto-SOAP-master
Copy-Item .env.example .env   # si existe; si no, crea el .env con las variables de abajo
.\iniciar-soap.ps1
```

Variables del mini cliente:

| Variable | Descripción |
|---|---|
| `DB_URL` / `DB_USERNAME` / `DB_PASSWORD` | Conexión a `VeterinariaMiniDB` |
| `VETSPHERE_SOAP_URL` | Default `http://localhost:8080/soap/productos` |
| `SOAP_USERNAME` / `SOAP_PASSWORD` | Credenciales WS-Security hacia el backend |
| `APP_API_TOKEN` | Token local (mínimo 32 caracteres) que protege `/api/**` del mini cliente vía cabecera `X-App-Token` |

El mini cliente expone `GET /api/stock/productos` (stock local + remoto vía SOAP) y `POST /api/stock/transferir` (transferencias con patrón de compensación).

---

## 7. Arranque completo con un solo comando

Desde la raíz del repositorio:

```powershell
.\iniciar-todo.ps1
```

Abre tres ventanas de PowerShell (backend, frontend y mini SOAP). Requisito previo: haber creado los `.env` de backend y mini cliente.

| Punto de acceso | URL |
|---|---|
| Frontend | `http://localhost:4200` |
| Backend REST | `http://localhost:8080/api` |
| Swagger UI | `http://localhost:8080/swagger-ui.html` |
| WSDL SOAP | `http://localhost:8080/soap/productos?wsdl` |
| Mini SOAP | `http://localhost:8090` |

---

## 8. Asistente de IA (chatbot)

1. Define `CHAT_API_KEY` en el `.env` del backend (cualquier proveedor compatible con OpenAI).
2. Opcional: `CHAT_BASE_URL` y `CHAT_MODEL`. OpenRouter: `https://openrouter.ai/api/v1` y `openai/gpt-4o-mini`; DeepSeek: `https://api.deepseek.com/v1`; Groq: `https://api.groq.com/openai/v1`.
3. Reinicia el backend. En el frontend aparece el botón flotante abajo a la derecha (en cualquier pantalla autenticada).

El asistente usa *function calling* sobre datos reales: responde cosas como *“¿cuántos productos tienen stock crítico?”, “¿qué citas hay pendientes?”, “buscar mascotas de la raza poodle”*. Sin clave configurada, el widget informa que el asistente no está habilitado.

---

## 9. Pruebas y calidad

```powershell
# Backend: JUnit 5 + Mockito + H2, y reporte de cobertura JaCoCo (target/site/jacoco)
$env:JAVA_HOME="C:\Program Files\JetBrains\IntelliJ IDEA <versión>\jbr"
& "C:\Program Files\JetBrains\IntelliJ IDEA <versión>\plugins\maven-plugin\lib\maven3\bin\mvn.cmd" -f .\Backend-Proyecto-Lenguajes-main\Backend-Proyecto-Lenguajes-main\pom.xml verify

# Frontend: Karma/Jasmine con Edge headless (no requiere instalar Chrome)
$env:CHROME_BIN="C:\Program Files (x86)\Microsoft\Edge\Application\msedge.exe"
cd .\Frontend-Proyecto-Lenguajes-main\Frontend-Proyecto-Lenguajes-main
npx ng test --watch=false --browsers=ChromeHeadless

# Verificación estructural del repositorio
python test_calidad_sistema.py

# Prueba manual del endpoint SOAP (requiere backend corriendo)
$env:SOAP_PASSWORD="tu-clave-soap"; python test_soap.py
```

---

## 10. Solución de problemas

| Síntoma | Causa probable | Solución |
|---|---|---|
| `Connection refused` en 8080 desde el frontend | Backend no arrancó o CORS | Revisa la consola del backend; verifica `CORS_ALLOWED_ORIGINS` |
| Error de autenticación SQL Server | Credenciales o puerto incorrectos | Verifica `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` y que el servicio SQL Server esté activo |
| El login devuelve error 500 | `JWT_SECRET` no definido | Define `JWT_SECRET` (mínimo 32 bytes) en `.env` |
| No llegan códigos 2FA por correo | SMTP no configurado | Define `MAIL_USERNAME`/`MAIL_PASSWORD` o usa recuperación con el código de la consola en entorno local |
| `ng test` no encuentra navegador | No hay Chrome | Usa `CHROME_BIN` apuntando a `msedge.exe` (ver sección 9) |
| Puerto ocupado (4200/8080/8090) | Otro proceso | Cierra el proceso o cambia el puerto en la configuración del servicio |
| El asistente responde “no está configurado” | Falta `CHAT_API_KEY` | Define la variable y reinicia el backend |
