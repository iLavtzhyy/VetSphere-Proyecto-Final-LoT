# VetSphere · Cliente SOAP de sucursal (Mini SOAP)

Aplicación Spring Boot independiente que representa el **inventario de una sucursal**. Sincroniza su stock con el backend principal de VetSphere mediante el servicio SOAP `/soap/productos` (WS-Security UsernameToken + PasswordDigest) y expone una pequeña UI de demostración.

**Puerto:** 8090 · **Base de datos:** `VeterinariaMiniDB` (SQL Server)

---

## Funcionalidad

| Recurso | Descripción |
|---|---|
| `GET /api/stock/productos` | Lista el stock local y el stock remoto (consultado por SOAP). Muestra "Desconectado" si el backend no responde. |
| `POST /api/stock/transferir` | Transferencias tipo *enviar* / *recibir* entre inventarios, con **patrón de compensación**: si falla la escritura local, se intenta revertir la operación remota. |
| `index.html` (UI estática) | Consola de inventario; solicita el token API y lo mantiene solo en memoria. |

Seguridad local: las rutas `/api/**` exigen la cabecera `X-App-Token` con valor igual a `APP_API_TOKEN` (mínimo 32 caracteres), comparado en tiempo constante.

---

## Variables de entorno

| Variable | Descripción |
|---|---|
| `DB_URL` / `DB_USERNAME` / `DB_PASSWORD` | Conexión a `VeterinariaMiniDB` |
| `VETSPHERE_SOAP_URL` | Default `http://localhost:8080/soap/productos` |
| `SOAP_USERNAME` / `SOAP_PASSWORD` | Credenciales WS-Security hacia el backend principal |
| `APP_API_TOKEN` | Token local (mín. 32 caracteres) para `/api/**` |

No guardes secretos en `.env.example` ni en Git; usa variables de entorno o un `.env` local ignorado por `.gitignore`.

## Ejecución

```powershell
# Opción A: script (carga .env si existe)
.\iniciar-soap.ps1

# Opción B: manual
mvn spring-boot:run
```

1. Ejecuta `scripts/Preparar-SqlServer.ps1` del backend para crear `VeterinariaDB` y `VeterinariaMiniDB`.
2. Inicia el **backend principal** en el puerto 8080 (debe estar corriendo antes de transferir stock).
3. Abre `http://localhost:8090` e ingresa el token configurado.

---

## ⚠️ Limitaciones conocidas

El token API es una protección local compartida para la demostración; no reemplaza cuentas individuales, auditoría de identidad ni TLS. Las transferencias entre dos bases siguen necesitando idempotencia y reconciliación (por ejemplo, saga con registro de operaciones) para evitar divergencias si una escritura falla a mitad de camino — riesgo declarado en la presentación del proyecto.
