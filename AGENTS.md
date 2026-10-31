# AGENTS.md - VetSphere

## Stack
- **Backend**: Spring Boot 3.3.5, Java 17, Maven. Modules under `Backend-Proyecto-Lenguajes-main/Backend-Proyecto-Lenguajes-main/`. REST + CXF SOAP 1.1 on `/soap`, WS-Security UsernameToken (PasswordDigest). JPA/Hibernate, SQL Server, JWT (jjwt 0.12.6), Lombok, Spring Security, Validation, Mail, OpenAPI (springdoc), Apache POI, OpenPDF, ZXing.
- **Frontend**: Angular 17 (standalone/router). `Frontend-Proyecto-Lenguajes-main/Frontend-Proyecto-Lenguajes-main/`. Runs on :4200.
- **Mini SOAP**: `MiniProyecto-SOAP-master/MiniProyecto-SOAP-master/` on :8090 (B2B/test service).
- **DB**: SQL Server Express local (default :1433). Schema `VeterinariaDB` expected.

## Run / Dev
- **Start everything (Windows)**: `.\iniciar-todo.ps1` (or `INICIAR_TODO.bat`). Requires JDK 17+, SQL Server Express (`MSSQL$SQLEXPRESS`) on 1433. Starts backend (:8080), frontend (:4200), mini SOAP (:8090). Logs in `logs/`.
- **DB prep**: `Backend-Proyecto-Lenguajes-main/Backend-Proyecto-Lenguajes-main/scripts/Preparar-SqlServer.ps1` (creates/initializes DB/schema as needed).
- **Backend only**: `Backend-Proyecto-Lenguajes-main/Backend-Proyecto-Lenguajes-main/iniciar-backend.ps1` (sets JAVA_HOME/JDK, runs Spring Boot).
- **Frontend only**: `Frontend-Proyecto-Lenguajes-main/Frontend-Proyecto-Lenguajes-main/iniciar-frontend.ps1` or `npm start` from that folder (`ng serve --host 0.0.0.0 --port 4200`).
- **SOAP only**: `MiniProyecto-SOAP-master/MiniProyecto-SOAP-master/iniciar-soap.ps1`.

## Build / Test
- **Backend tests + coverage**: from backend dir run `mvn verify`. JaCoCo is configured (prepare-agent on test, report/check on verify). Check requires >=50% line coverage (see `pom.xml`). Surefire passes `-Dnet.bytebuddy.experimental=true -XX:+EnableDynamicAgentLoading`. Tests use H2 (see `pom.xml`).
- **Frontend tests**: from frontend dir `npm ci && npx ng test --watch=false` (package.json script is `"test": "ng test --watch=false"`).
- **Frontend build**: `npm run build` (Angular 17).
- **Python/system tests**: `test_calidad_sistema.py`, `test_soap.py` exist at repo root (integration-style checks).

## Configuration
- **Env**: `.env` at repo root (required). Key vars (see `application.properties`): `DB_URL/DB_USERNAME/DB_PASSWORD`, `JWT_SECRET`, `SOAP_USERNAME/SOAP_PASSWORD`, `MAIL_USERNAME/MAIL_PASSWORD`, `CORS_ALLOWED_ORIGINS`, `CHAT_API_KEY` (optional), `ADMIN_REGISTRATION_CODE`, `DEMO_SEED_PASSWORD`, tarifas (`TARIFA_*`), clinic data, security limits.
- **Profiles**: `spring.profiles.active=${SPRING_PROFILES_ACTIVE:local}`.
- **Uploads**: `app.upload-dir=uploads`, multipart limits 20MB/30MB. Files served via `/uploads/**` (requires auth).
- **AI**: `/api/chat` works without `CHAT_API_KEY` but returns "not configured" message (no crash).

## Architecture (critical)
- **AuthZ single source of truth**: `security/ReglasSeguridad.java` defines `REGLAS` (ordered; first match wins). `config/SecurityConfig.java` builds the filter chain by iterating `REGLAS` and translates modes (`PUBLICO`/`AUTENTICADO`/`ROLES`). `PermisoController` derives permission matrix from the same table. **Do not duplicate URL/role rules elsewhere**.
- **Roles**: `ADMIN`, `VETERINARIO`, `RECEPCIONISTA`, `CLIENTE` (normalized via `security/Roles`). Public: `/api/auth/**`, `/api/veterinarios/publico`, `/soap/**`, `/error`. Swagger only for ADMIN in practice. `mi-cuenta/**` requires auth for any authenticated user.
- **JWT**: stateless, filter in chain before `UsernamePasswordAuthenticationFilter`. Role revalidation is enforced (see README). Disabled users cannot authenticate.
- **REST wrapper**: API responses follow `{ok, mensaje, data}` pattern (common across controllers).
- **Business rules enforced server-side**: totals, discounts, prices, coupon validation computed on backend (never trust frontend values).
- **SOAP**: CXF at `/soap`, endpoint `/soap/productos` with WS-Security PasswordDigest. Configured via `vetsphere.soap.*`.
- **CORS**: configured from `app.cors.allowed-origins` (defaults localhost:4200/127.0.0.1:4200).
- **Frontend guards**: routes use `authGuard` with role restrictions matching backend; permission matrix derived from `ReglasSeguridad` so UI/backend stay aligned.

## Gotchas
- **Order matters**: in `ReglasSeguridad.REGLAS`, more specific rules must come before general ones (Spring Security first-match-wins).
- **Passwords/keys required**: `JWT_SECRET`, `DB_USERNAME/PASSWORD`, `SOAP_USERNAME/PASSWORD` must be set in `.env` for normal operation.
- **SQL Server trust**: connection uses `encrypt=true;trustServerCertificate=false` by default (adjust for local certs if needed).
- **JaCoCo argLine**: surefire uses `@{argLine}` plus ByteBuddy/Mockito flags for JDK compatibility.
- **No multi-tenant/ClinicId or PetMatch/propietario portal in current codebase** - treat as proposed ideas only.
- **Windows-first scripts**: `iniciar-todo.ps1` uses PowerShell cmdlets; run from repo root.
