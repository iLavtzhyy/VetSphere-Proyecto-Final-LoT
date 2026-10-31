# Revisión técnica de seguridad VetSphere

Estado inicial de revisión de código, no certificado de conformidad. La ISO/IEC 27001 requiere un SGSI organizacional basado en alcance, riesgos, controles aplicables, evidencias y mejora continua. ISO/IEC 27010 añade directrices para intercambios de información entre organizaciones. Esta revisión cubre únicamente los tres repositorios compartidos.

## Cambios técnicos aplicados

- Las propiedades de los backends ya no contienen la contraseña SQL, la contraseña SOAP, el secreto JWT ni la contraseña SMTP observados en los archivos originales. Se leen desde variables de entorno.
- La conexión SQL exige cifrado y deja de confiar automáticamente en cualquier certificado del servidor. En desarrollo local se debe instalar un certificado confiable; no habilitar `trustServerCertificate=true` como solución permanente.
- Las credenciales locales tienen plantillas `.env.example`; los `.env` quedan ignorados por Git.
- El código fijo para autorizar registros de personal se retiró y ahora se lee desde `ADMIN_REGISTRATION_CODE`, comparado en tiempo constante. Sin configurar el valor, el alta de personal queda denegada.
- La impresión de SQL queda desactivada en el perfil local.

## Hallazgos que siguen abiertos

| Prioridad | Hallazgo | Riesgo / trabajo necesario |
|---|---|---|
| Crítica | Los repositorios incluían credenciales compartidas/reutilizadas y el seeder crea cuentas con una contraseña conocida | Rotar las claves expuestas, eliminar el uso de cuentas compartidas y migrar usuarios de demo antes de cualquier despliegue. No reutilizar la clave publicada en el chat. |
| Alta | La instancia SQL local está detenida y el puerto 1433 no responde | La base no se pudo crear ni probar. La solicitud elevada para iniciar el servicio fue rechazada por Windows. Configurar autenticación mixta y un login de aplicación de mínimo privilegio cuando el servicio pueda iniciarse. |
| Alta | El backend de la clínica usa `ddl-auto=update` en perfil local y no tiene migraciones versionadas | Añadir Flyway/Liquibase; usar `validate` en despliegue y separar permisos de migración de permisos de ejecución. |
| Alta | El token JWT del frontend se almacena en `localStorage` | Cualquier XSS puede robarlo. Migrar a cookie `HttpOnly`, `Secure`, `SameSite` con protección CSRF y pruebas de sesión. |
| Alta | Se generan códigos de verificación con `ThreadLocalRandom`; no hay límite visible de intentos | Usar `SecureRandom`, limitar solicitudes e intentos, invalidar códigos anteriores, proteger contra enumeración de usuarios y registrar eventos sin guardar códigos. |
| Alta | Las transferencias SOAP remota/local no son transaccionales y el cliente mini devuelve solo resultados parciales | Verificar respuesta/Fault del SOAP, idempotencia, concurrencia, límites de stock y compensación/reconciliación tras fallos entre bases. Añadir pruebas de fallo en cada paso. |
| Alta | El endpoint SOAP se permite sin autenticación HTTP y usa HTTP local | Validar WS-Security de extremo a extremo, aplicar TLS con certificados confiables y pruebas negativas de nonce/fecha/firma/usuario. No publicar SOAP por HTTP fuera de localhost. |
| Media | La lista de extensiones de subida no valida contenido real y los archivos se sirven desde una ruta pública | Validar tipo por contenido, tamaño y antivirus; almacenar fuera del webroot, exigir autorización de descarga y probar traversal y archivos poliglota. |
| Media | El frontend contiene páginas de demostración/credenciales y guarda roles en almacenamiento modificable por el cliente | Retirar credenciales de demo, tratar el rol del frontend solo como ayuda visual y probar autorización de cada endpoint exclusivamente en backend. |
| Media | No se encontraron pruebas automatizadas de backend en `src/test` (solo XML de solicitudes SOAP) | Incorporar pruebas de autorización, autenticación, validación, auditoría, recuperación, XML/XXE, archivos y transferencias. |
| Crítica | `pnpm audit` reportó 65 avisos en el árbol del frontend: 1 crítico, 32 altos, 28 moderados y 4 bajos; incluye dependencias Angular vulnerables | El proyecto usa Angular 17, fuera de soporte. `pnpm audit --fix=update` no resolvió avisos por estar bloqueados por rangos de versión. Migrar Angular por cada major hasta una versión soportada y repetir pruebas/auditoría antes de publicar. |

## Verificaciones de ISO/IEC 27010 para intercambio

La integración SOAP necesita evidencias de: propietario y clasificación de los datos compartidos; propósito autorizado; acuerdos y responsabilidades con la otra organización; autenticación mutua; cifrado en tránsito; integridad y protección contra repetición; minimización de datos; retención y eliminación; notificación y tratamiento de incidentes; continuidad y reconciliación de inventario. Estos requisitos operativos no pueden demostrarse solo leyendo el código.

## Próxima verificación ejecutable

1. Rotar las contraseñas de SQL, SMTP, SOAP y JWT que aparecieron en archivos o conversaciones; configurar secretos nuevos fuera del repositorio.
2. Iniciar SQL Server con una cuenta administradora del equipo y crear `VeterinariaDB` y `VeterinariaMiniDB`; crear un login separado para la aplicación y conceder solo permisos necesarios.
3. Configurar las variables de `.env.example` en el entorno local y usar un certificado TLS de SQL confiable.
4. Ejecutar `mvn verify` para los dos backends y `pnpm test` más `pnpm build` para Angular. En esta sesión no están disponibles Maven ni el servicio SQL, por eso los backends y la conexión todavía no se han verificado.
5. Ejecutar pruebas de integración y seguridad sobre ambos procesos Spring y guardar resultados, logs redactados y versión de código como evidencia.
