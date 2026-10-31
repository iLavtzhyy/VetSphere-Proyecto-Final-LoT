# VetSphere · Frontend

Aplicación Angular 17 (componentes standalone, TypeScript estricto, Tailwind CSS) del sistema de información clínica veterinaria **VetSphere**.

---

## ▶️ Arranque rápido

```powershell
npm install
npm start          # http://localhost:4200  (o: .\iniciar-frontend.ps1)
```

Requisitos: Node 18+ (probado con 24) y el backend corriendo en `http://localhost:8080`.

**Configuración del API:** la URL base vive en `src/environments/environment.ts` (`apiUrl`). Para otro dominio, edita `environment.prod.ts` y compila con `npm run build` (el reemplazo de entornos está registrado en `angular.json`).

---

## 🖥️ Pantallas

| Ruta | Pantalla | Roles |
|---|---|---|
| `/` | Landing pública (servicios, equipo, FAQ, contacto) | público |
| `/login`, `/registro`, `/verificacion` | Autenticación, registro y verificación 2FA | público |
| `/dashboard` | KPIs, alertas, gráficos | ADMIN, VETERINARIO, RECEPCIONISTA |
| `/mascotas` | Pacientes, ficha con línea de tiempo clínica y adjuntos | todos (clientes: solo los suyos) |
| `/clientes` | CRUD de clientes y gasto total | ADMIN, RECEPCIONISTA |
| `/consultas`, `/cirugias`, `/laboratorio`, `/hospitalizacion` | Módulos clínicos | ADMIN, VETERINARIO |
| `/recepcion` | Agenda y citas | ADMIN, RECEPCIONISTA |
| `/farmacia` | Inventario, stock crítico, ventas y panel SOAP | ADMIN, RECEPCIONISTA |
| `/facturacion` | Facturación con IVA, pagos y comprobantes imprimibles | ADMIN, RECEPCIONISTA |
| `/reportes` | Historiales imprimibles, PDF y Excel | ADMIN |
| `/usuarios`, `/administracion` | Usuarios, roles y aprobación de solicitudes | ADMIN |
| `/veterinarios` | Equipo veterinario (edición: ADMIN) | todos |
| `/calidad-software` | Informe de calidad ISO 25010/27001 imprimible | ADMIN |
| widget flotante 💬 | **Asistente de IA** (consultas con datos reales del backend) | todos los autenticados |

---

## 📁 Estructura

```
src/app/
├── app.routes.ts          → rutas + guard de roles (authGuard con data.roles)
├── app.config.ts          → router, animations, HttpClient con tokenInterceptor
├── core/
│   ├── api.service.ts     → ~70 métodos REST (incluye /api/chat del asistente)
│   ├── autorizacion.service.ts  → roles y sesión (localStorage)
│   ├── token.interceptor.ts     → Authorization: Bearer + cierre en 401
│   ├── sesion-inactividad.service.ts → expiración por inactividad (30 min)
│   ├── theme.service.ts   → modo oscuro (señal + clase 'dark')
│   └── notificacion.service.ts  → toasts de éxito/error
├── shared/components/     → icono, cargador, toast
└── components/            → 21 pantallas + chatbot (cada una .ts/.html/.css/.spec.ts)
```

---

## ✅ Pruebas

```powershell
# Con Edge headless (no requiere instalar Chrome):
$env:CHROME_BIN="C:\Program Files (x86)\Microsoft\Edge\Application\msedge.exe"
npx ng test --watch=false --browsers=ChromeHeadless
```

Suites con lógica real: `token.interceptor`, `autorizacion.service`, `auth.guard`, `api.service`, `theme.service`, `login` y `chatbot`; más pruebas de creación de cada componente.

---

## 🎨 Sistema de diseño

- Tailwind con paleta personalizada (`oceano`, `menta`, `tinta`) y **modo oscuro** por clase `dark`.
- Clases globales reutilizables en `src/styles.css` (`.panel`, `.campo`, `.boton-*`, `.data-table`, `.badge-*`, `.stat-card`).
- Componentes accesibles: `aria-label` en botones de icono, `aria-live` en notificaciones, etiquetas asociadas a campos, cierre de modales con `Escape`.
- Responsive con *container queries*, sidebar *off-canvas* y estilos de impresión para comprobantes e historiales.

---

## 🤖 Asistente de IA

Botón flotante (esquina inferior derecha) en todas las pantallas autenticadas. Consume `POST /api/chat` del backend; si el servidor no tiene `CHAT_API_KEY` configurada, muestra el mensaje de "no configurado" sin romper la interfaz.
