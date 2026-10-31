from __future__ import annotations

import asyncio
import hashlib
import logging
import os
import random
import secrets
from datetime import datetime, timezone
from typing import Any, Literal

import jwt
import pyodbc
from fastapi import Depends, FastAPI, HTTPException, Query, WebSocket, WebSocketDisconnect
from fastapi.middleware.cors import CORSMiddleware
from fastapi.security import HTTPAuthorizationCredentials, HTTPBearer
from fastapi.responses import JSONResponse
from fastapi.responses import StreamingResponse
from pydantic import BaseModel, Field, model_validator
from io import BytesIO

from .database import connect, connection, initialize_schema
from .models import SensorReading
from .simulation import SimulationEngine


logging.basicConfig(level=os.getenv("IOT_LOG_LEVEL", "INFO"))
logger = logging.getLogger("vetsphere.cold-chain")
app = FastAPI(title="VetSphere · Cadena de frío IoT", version="1.0.0")
app.add_middleware(
    CORSMiddleware,
    allow_origins=[value.strip() for value in os.getenv("CORS_ALLOWED_ORIGINS", "http://localhost:4200,http://127.0.0.1:4200").split(",") if value.strip()],
    allow_credentials=True,
    allow_methods=["GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"],
    allow_headers=["Authorization", "Content-Type"],
)

bearer = HTTPBearer(auto_error=False)
jwt_secret = os.getenv("JWT_SECRET", "")
simulation: SimulationEngine | None = None
websockets: set[WebSocket] = set()
main_loop: asyncio.AbstractEventLoop | None = None
monitor_task: asyncio.Task | None = None


class RefrigeratorInput(BaseModel):
    code: str = Field(min_length=2, max_length=40, pattern=r"^[A-Za-z0-9_-]+$")
    name: str = Field(min_length=2, max_length=120)
    location: str = Field(min_length=2, max_length=180)
    capacity: str | None = Field(default=None, max_length=80)
    min_temp: float = Field(ge=-40, le=40)
    max_temp: float = Field(ge=-40, le=60)

    @model_validator(mode="after")
    def valid_range(self):
        if self.min_temp >= self.max_temp:
            raise ValueError("La temperatura mínima debe ser menor que la máxima.")
        return self


class RefrigeratorPatch(BaseModel):
    name: str | None = Field(default=None, min_length=2, max_length=120)
    location: str | None = Field(default=None, min_length=2, max_length=180)
    capacity: str | None = Field(default=None, max_length=80)
    min_temp: float | None = Field(default=None, ge=-40, le=40)
    max_temp: float | None = Field(default=None, ge=-40, le=60)
    active: bool | None = None


class SensorInput(BaseModel):
    device_id: str = Field(min_length=3, max_length=80, pattern=r"^[A-Za-z0-9_-]+$")
    refrigerator_id: int = Field(gt=0)
    sensor_type: str = Field(default="DS18B20", min_length=2, max_length=80)


class SimulationSettings(BaseModel):
    interval_seconds: int = Field(default=5, ge=5, le=10)
    mode: Literal["NORMAL", "WARNING", "CRITICAL"] = "NORMAL"


class AlertActionInput(BaseModel):
    status: Literal["ATTENDED", "RESOLVED"]
    note: str = Field(min_length=3, max_length=1000)


class StorageInput(BaseModel):
    product_code: str = Field(min_length=1, max_length=80)
    refrigerator_id: int = Field(gt=0)


class StorageReviewInput(BaseModel):
    review_required: bool
    note: str = Field(min_length=3, max_length=1000)


def _to_dict(cursor, row) -> dict[str, Any]:
    result = dict(zip([column[0] for column in cursor.description], row))
    for key, value in result.items():
        if isinstance(value, datetime):
            result[key] = value.replace(tzinfo=timezone.utc).isoformat()
    return result


def _rows(cursor) -> list[dict[str, Any]]:
    return [_to_dict(cursor, row) for row in cursor.fetchall()]


def _validate_admin_token(token: str) -> dict[str, Any]:
    if not token or not jwt_secret:
        raise HTTPException(status_code=401, detail="Inicia sesión como administrador.")
    try:
        # JJWT selects HS256/384/512 from the signing key length when the
        # backend calls signWith(key). Accept the HMAC variants only so this
        # service validates the same VetSphere JWT without permitting alg=none
        # or asymmetric algorithm confusion.
        claims = jwt.decode(token, jwt_secret, algorithms=["HS256", "HS384", "HS512"])
    except jwt.PyJWTError:
        raise HTTPException(status_code=401, detail="La sesión no es válida o ya venció.") from None

    # Igual que Spring Security, valida el estado y los permisos actuales desde
    # SQL Server para revocar inmediatamente tokens de cuentas bloqueadas,
    # desactivadas o que perdieron el rol ADMIN.
    email = claims.get("sub")
    if not isinstance(email, str) or not email.strip():
        raise HTTPException(status_code=401, detail="La sesión no identifica una cuenta válida.")
    with connection() as cn:
        cursor = cn.cursor()
        cursor.execute(
            "SELECT u.activo,u.bloqueado_hasta,"
            "CAST(CASE WHEN EXISTS(SELECT 1 FROM dbo.usuario_roles ur "
            "JOIN dbo.rol r ON r.id=ur.rol_id WHERE ur.usuario_id=u.id AND r.nombre='ROLE_ADMIN') "
            "THEN 1 ELSE 0 END AS BIT) AS is_admin "
            "FROM dbo.usuario u WHERE LOWER(u.email)=LOWER(?)",
            email.strip(),
        )
        user = cursor.fetchone()
    if not user or not user.activo or (user.bloqueado_hasta and user.bloqueado_hasta > datetime.now()):
        raise HTTPException(status_code=401, detail="La cuenta está inactiva o bloqueada.")
    if not user.is_admin:
        raise HTTPException(status_code=403, detail="Este módulo requiere permisos de administrador.")
    return claims


def _auth_user(credentials: HTTPAuthorizationCredentials | None) -> dict[str, Any]:
    if not credentials:
        raise HTTPException(status_code=401, detail="Inicia sesión como administrador.")
    return _validate_admin_token(credentials.credentials)


def require_admin(credentials: HTTPAuthorizationCredentials | None = Depends(bearer)) -> dict[str, Any]:
    return _auth_user(credentials)


def _device_sensor(device_id: str, token: str, cn):
    cursor = cn.cursor()
    cursor.execute("SELECT id,refrigerator_id,status,simulated,token_hash FROM dbo.iot_sensors WHERE device_id=?", device_id)
    row = cursor.fetchone()
    if not row or row.status == "INACTIVE" or row.simulated:
        raise HTTPException(status_code=401, detail="Sensor no autorizado o inactivo.")
    digest = hashlib.sha256(token.encode("utf-8")).hexdigest()
    if not row.token_hash or not secrets.compare_digest(row.token_hash.strip(), digest):
        raise HTTPException(status_code=401, detail="Credencial de sensor incorrecta.")
    return row


async def _broadcast(event: dict[str, Any]) -> None:
    stale: list[WebSocket] = []
    for socket in tuple(websockets):
        try:
            await socket.send_json(event)
        except Exception:
            stale.append(socket)
    for socket in stale:
        websockets.discard(socket)


def _alert(cursor, refrigerator_id: int, sensor_id: int | None, reading_id: int | None,
           kind: str, severity: str, message: str, temperature: float | None) -> int:
    cursor.execute(
        "SELECT TOP 1 id FROM dbo.iot_alerts WHERE sensor_id=? AND alert_type=? AND status<>'RESOLVED' ORDER BY id DESC",
        sensor_id, kind,
    )
    existing = cursor.fetchone()
    now = datetime.now(timezone.utc).replace(tzinfo=None)
    if existing:
        alert_id = existing.id
        cursor.execute(
            "UPDATE dbo.iot_alerts SET reading_id=?,severity=?,message=?,temperature=?,updated_at=? WHERE id=?",
            reading_id, severity, message, temperature, now, alert_id,
        )
    else:
        cursor.execute(
            "INSERT INTO dbo.iot_alerts(refrigerator_id,sensor_id,reading_id,alert_type,severity,status,message,temperature,created_at,updated_at) "
            "OUTPUT INSERTED.id VALUES(?,?,?,?,?,'PENDING',?,?,?,?)",
            refrigerator_id, sensor_id, reading_id, kind, severity, message, temperature, now, now,
        )
        alert_id = cursor.fetchone()[0]
    if severity == "CRITICAL" and reading_id:
        cursor.execute("UPDATE dbo.iot_product_storage SET review_required=1,last_review_alert_id=?,updated_at=? WHERE refrigerator_id=?",
                       alert_id, now, refrigerator_id)
    return int(alert_id)


def _classify_temperature(temperature: float, low: float, high: float) -> tuple[str, str, str] | None:
    if temperature < low or temperature > high:
        return "OUT_OF_RANGE", "CRITICAL", f"Temperatura fuera del rango seguro ({low:.1f} a {high:.1f} °C)."
    if temperature - low <= 0.5 or high - temperature <= 0.5:
        return "NEAR_LIMIT", "WARNING", "Temperatura próxima al límite configurado; verificar la nevera."
    return None


def _save_reading(payload: SensorReading, source: str, internal_simulation: bool = False) -> dict[str, Any]:
    observed_at = payload.timestamp
    if observed_at.tzinfo is None:
        observed_at = observed_at.replace(tzinfo=timezone.utc)
    observed_at = observed_at.astimezone(timezone.utc).replace(tzinfo=None)
    with connection() as cn:
        cursor = cn.cursor()
        cursor.execute(
            "SELECT s.id,s.refrigerator_id,s.status,s.simulated,r.name,r.min_temp,r.max_temp "
            "FROM dbo.iot_sensors s JOIN dbo.iot_refrigerators r ON r.id=s.refrigerator_id "
            "WHERE s.device_id=? AND r.active=1", payload.device_id,
        )
        sensor = cursor.fetchone()
        if not sensor:
            raise HTTPException(status_code=404, detail="No existe el sensor o la nevera está desactivada.")
        if internal_simulation and not sensor.simulated:
            raise HTTPException(status_code=403, detail="El simulador solo puede generar lecturas para sensores SIMULADOS.")
        if not internal_simulation and sensor.simulated:
            raise HTTPException(status_code=403, detail="Los sensores SIMULADOS solo aceptan lecturas del simulador.")
        if sensor.status == "INACTIVE":
            raise HTTPException(status_code=409, detail="El sensor está desactivado por el administrador.")
        cursor.execute("SELECT TOP 1 id FROM dbo.iot_temperature_readings WHERE sensor_id=? AND observed_at=? AND source=?", sensor.id, observed_at, source)
        if cursor.fetchone():
            raise HTTPException(status_code=409, detail="Esta medición ya fue recibida; se omitió el duplicado.")
        cursor.execute(
            "INSERT INTO dbo.iot_temperature_readings(sensor_id,temperature,humidity,observed_at,source) "
            "OUTPUT INSERTED.id VALUES(?,?,?,?,?)", sensor.id, payload.temperature, payload.humidity,
            observed_at, source,
        )
        reading_id = int(cursor.fetchone()[0])
        received = datetime.now(timezone.utc).replace(tzinfo=None)
        cursor.execute("UPDATE dbo.iot_sensors SET last_seen=?,status='ACTIVE' WHERE id=?", received, sensor.id)
        condition = _classify_temperature(payload.temperature, float(sensor.min_temp), float(sensor.max_temp))
        alert_data = None
        if condition:
            kind, severity, message = condition
            alert_id = _alert(cursor, sensor.refrigerator_id, sensor.id, reading_id, kind, severity,
                              f"{sensor.name}: {message}", payload.temperature)
            alert_data = {"id": alert_id, "type": kind, "severity": severity, "status": "PENDING", "message": f"{sensor.name}: {message}"}
        cursor.execute(
            "SELECT TOP 1 temperature FROM dbo.iot_temperature_readings WHERE sensor_id=? AND id<>? ORDER BY observed_at DESC,id DESC",
            sensor.id, reading_id,
        )
        previous = cursor.fetchone()
        if previous and abs(payload.temperature - float(previous.temperature)) >= 2:
            alert_id = _alert(cursor, sensor.refrigerator_id, sensor.id, reading_id, "RAPID_CHANGE", "WARNING",
                              f"{sensor.name}: cambio brusco de temperatura detectado.", payload.temperature)
            alert_data = alert_data or {"id": alert_id, "type": "RAPID_CHANGE", "severity": "WARNING", "status": "PENDING",
                                        "message": f"{sensor.name}: cambio brusco de temperatura detectado."}
        result = {
            "id": reading_id, "sensorId": sensor.id, "deviceId": payload.device_id,
            "refrigeratorId": sensor.refrigerator_id, "refrigerator": sensor.name,
            "temperature": payload.temperature, "humidity": payload.humidity,
            "timestamp": observed_at.replace(tzinfo=timezone.utc).isoformat(),
            "receivedAt": received.replace(tzinfo=timezone.utc).isoformat(),
            "source": source, "minTemp": float(sensor.min_temp), "maxTemp": float(sensor.max_temp),
            "condition": condition[1] if condition else "NORMAL", "alert": alert_data,
        }
    return result


def _simulation_sensors() -> list[tuple]:
    with connection() as cn:
        cursor = cn.cursor()
        cursor.execute(
            "SELECT s.id,s.device_id,s.status,r.id,r.min_temp,r.max_temp "
            "FROM dbo.iot_sensors s JOIN dbo.iot_refrigerators r ON r.id=s.refrigerator_id "
            "WHERE s.simulated=1 AND r.active=1 ORDER BY s.id"
        )
        return cursor.fetchall()


def _mark_stale_real_sensors() -> list[dict[str, Any]]:
    affected: list[dict[str, Any]] = []
    with connection() as cn:
        cursor = cn.cursor()
        cursor.execute("SELECT s.id,s.device_id,s.refrigerator_id,r.name FROM dbo.iot_sensors s JOIN dbo.iot_refrigerators r ON r.id=s.refrigerator_id WHERE s.status='ACTIVE' AND s.simulated=0 AND ((s.last_seen IS NOT NULL AND s.last_seen<DATEADD(second,-30,SYSUTCDATETIME())) OR (s.last_seen IS NULL AND s.installed_at<DATEADD(second,-30,SYSUTCDATETIME())))")
        for sensor_id, device_id, refrigerator_id, refrigerator in cursor.fetchall():
            cursor.execute("UPDATE dbo.iot_sensors SET status='DISCONNECTED' WHERE id=? AND status='ACTIVE'", sensor_id)
            if cursor.rowcount:
                message = f"El sensor {device_id} de {refrigerator} no comunica desde hace más de 30 segundos."
                alert_id = _alert(cursor, refrigerator_id, sensor_id, None, "SENSOR_OFFLINE", "CRITICAL", message, None)
                affected.append({"id": alert_id, "type": "SENSOR_OFFLINE", "severity": "CRITICAL", "status": "PENDING", "message": message})
    return affected


async def _monitor_sensor_connections() -> None:
    while True:
        try:
            alerts = await asyncio.to_thread(_mark_stale_real_sensors)
            for alert in alerts:
                await _broadcast({"type": "alert", "data": alert})
        except asyncio.CancelledError:
            raise
        except Exception:
            logger.exception("No se pudo revisar la comunicación de los sensores físicos")
        await asyncio.sleep(10)


def _set_sensor_status(sensor_id: int, status: str, user_email: str | None = None) -> dict[str, Any]:
    with connection() as cn:
        cursor = cn.cursor()
        cursor.execute("SELECT id,device_id,refrigerator_id,simulated,status FROM dbo.iot_sensors WHERE id=?", sensor_id)
        sensor = cursor.fetchone()
        if not sensor or not sensor.simulated:
            raise HTTPException(status_code=404, detail="No se encontró un sensor SIMULADO.")
        cursor.execute("UPDATE dbo.iot_sensors SET status=? WHERE id=?", status, sensor_id)
        alert = None
        if status == "DISCONNECTED":
            message = f"El sensor {sensor.device_id} no está comunicando."
            alert_id = _alert(cursor, sensor.refrigerator_id, sensor_id, None, "SENSOR_OFFLINE", "CRITICAL", message, None)
            alert = {"id": alert_id, "type": "SENSOR_OFFLINE", "severity": "CRITICAL", "status": "PENDING", "message": message}
        return {"id": sensor.id, "deviceId": sensor.device_id, "status": status, "alert": alert}


@app.on_event("startup")
async def startup() -> None:
    global main_loop, simulation, monitor_task
    main_loop = asyncio.get_running_loop()
    await asyncio.to_thread(initialize_schema)
    simulation = SimulationEngine(_simulation_sensors, _save_reading, _broadcast)
    monitor_task = asyncio.create_task(_monitor_sensor_connections(), name="vetsphere-iot-connection-monitor")


@app.on_event("shutdown")
async def shutdown() -> None:
    if simulation:
        await simulation.stop()
    if monitor_task and not monitor_task.done():
        monitor_task.cancel()
        try:
            await monitor_task
        except asyncio.CancelledError:
            pass


@app.exception_handler(pyodbc.Error)
async def database_error(_, exc: pyodbc.Error):
    logger.error("La operación SQL Server del módulo IoT falló: %s", type(exc).__name__)
    return JSONResponse(
        status_code=503, content={"detail": "No se pudo guardar o consultar la cadena de frío en SQL Server."}
    )


@app.get("/api/iot/health")
def health():
    with connection() as cn:
        cn.cursor().execute("SELECT 1")
    return {"ok": True, "service": "vetsphere-cold-chain", "database": "connected"}


@app.get("/api/iot/admin/dashboard")
def dashboard(_: dict = Depends(require_admin)):
    with connection() as cn:
        cursor = cn.cursor()
        cursor.execute("""
            SELECT r.id refrigeratorId,r.code,r.name,r.location,r.capacity,r.min_temp minTemp,r.max_temp maxTemp,r.active,
              (SELECT COUNT(*) FROM dbo.iot_sensors s WHERE s.refrigerator_id=r.id) sensorCount,
              (SELECT COUNT(*) FROM dbo.iot_sensors s WHERE s.refrigerator_id=r.id AND s.status='ACTIVE') activeSensors,
              (SELECT TOP 1 s.status FROM dbo.iot_sensors s WHERE s.refrigerator_id=r.id ORDER BY s.id) sensorStatus,
              (SELECT TOP 1 s.device_id FROM dbo.iot_sensors s WHERE s.refrigerator_id=r.id ORDER BY s.id) deviceId,
              (SELECT TOP 1 s.last_seen FROM dbo.iot_sensors s WHERE s.refrigerator_id=r.id ORDER BY s.id) lastSeen,
              (SELECT TOP 1 rd.temperature FROM dbo.iot_sensors s JOIN dbo.iot_temperature_readings rd ON rd.sensor_id=s.id
                 WHERE s.refrigerator_id=r.id ORDER BY rd.observed_at DESC,rd.id DESC) temperature,
              (SELECT TOP 1 rd.observed_at FROM dbo.iot_sensors s JOIN dbo.iot_temperature_readings rd ON rd.sensor_id=s.id
                 WHERE s.refrigerator_id=r.id ORDER BY rd.observed_at DESC,rd.id DESC) measuredAt,
              (SELECT COUNT(*) FROM dbo.iot_alerts a WHERE a.refrigerator_id=r.id AND a.status<>'RESOLVED') openAlerts,
              CAST(CASE WHEN EXISTS(SELECT 1 FROM dbo.iot_sensors s JOIN dbo.iot_temperature_readings rd ON rd.sensor_id=s.id
                 WHERE s.refrigerator_id=r.id AND rd.observed_at>=DATEADD(second,-30,SYSUTCDATETIME())
                   AND rd.temperature>=r.min_temp AND rd.temperature<=r.max_temp) THEN 1 ELSE 0 END AS BIT) normalNow
            FROM dbo.iot_refrigerators r WHERE r.active=1 ORDER BY r.id
        """)
        refrigerators = _rows(cursor)
        cursor.execute("SELECT COUNT(*) FROM dbo.iot_refrigerators WHERE active=1")
        refrigerator_count = cursor.fetchone()[0]
        cursor.execute("SELECT COUNT(*) FROM dbo.iot_sensors WHERE status='ACTIVE'")
        active_sensors = cursor.fetchone()[0]
        cursor.execute("SELECT COUNT(*) FROM dbo.iot_sensors WHERE status<>'ACTIVE'")
        disconnected_sensors = cursor.fetchone()[0]
        cursor.execute("SELECT COUNT(*) FROM dbo.iot_alerts WHERE status<>'RESOLVED' AND severity='CRITICAL'")
        critical_alerts = cursor.fetchone()[0]
        cursor.execute("SELECT COUNT(*) FROM dbo.iot_refrigerators WHERE active=1 AND EXISTS(SELECT 1 FROM dbo.iot_sensors s JOIN dbo.iot_temperature_readings rd ON rd.sensor_id=s.id WHERE s.refrigerator_id=dbo.iot_refrigerators.id AND rd.observed_at>=DATEADD(second,-30,SYSUTCDATETIME()) AND rd.temperature BETWEEN dbo.iot_refrigerators.min_temp AND dbo.iot_refrigerators.max_temp)")
        normal_refrigerators = cursor.fetchone()[0]
    return {"refrigeratorCount": refrigerator_count, "activeSensors": active_sensors,
            "disconnectedSensors": disconnected_sensors, "normalRefrigerators": normal_refrigerators,
            "criticalAlerts": critical_alerts, "refrigerators": refrigerators,
            "simulation": simulation.status()}


@app.get("/api/iot/admin/refrigerators")
def list_refrigerators(_: dict = Depends(require_admin)):
    with connection() as cn:
        cursor = cn.cursor()
        cursor.execute("SELECT id,code,name,location,capacity,min_temp minTemp,max_temp maxTemp,active,created_at createdAt FROM dbo.iot_refrigerators ORDER BY id")
        return _rows(cursor)


@app.post("/api/iot/admin/refrigerators", status_code=201)
def create_refrigerator(body: RefrigeratorInput, _: dict = Depends(require_admin)):
    with connection() as cn:
        cursor = cn.cursor()
        cursor.execute("INSERT INTO dbo.iot_refrigerators(code,name,location,capacity,min_temp,max_temp) OUTPUT INSERTED.id VALUES(?,?,?,?,?,?)",
                       body.code, body.name, body.location, body.capacity, body.min_temp, body.max_temp)
        new_id = int(cursor.fetchone()[0])
    return {"id": new_id, **body.model_dump(), "active": True}


@app.patch("/api/iot/admin/refrigerators/{refrigerator_id}")
def update_refrigerator(refrigerator_id: int, body: RefrigeratorPatch, _: dict = Depends(require_admin)):
    fields = body.model_dump(exclude_unset=True)
    if not fields:
        raise HTTPException(status_code=400, detail="No hay cambios para guardar.")
    with connection() as cn:
        cursor = cn.cursor()
        cursor.execute("SELECT min_temp,max_temp FROM dbo.iot_refrigerators WHERE id=?", refrigerator_id)
        current = cursor.fetchone()
        if not current:
            raise HTTPException(status_code=404, detail="Nevera no encontrada.")
        low = fields.get("min_temp", current.min_temp)
        high = fields.get("max_temp", current.max_temp)
        if low >= high:
            raise HTTPException(status_code=422, detail="La temperatura mínima debe ser menor que la máxima.")
        assignments = []
        params: list[Any] = []
        for key, value in fields.items():
            column = {"min_temp": "min_temp", "max_temp": "max_temp"}.get(key, key)
            assignments.append(f"{column}=?")
            params.append(value)
        assignments.append("updated_at=SYSUTCDATETIME()")
        params.append(refrigerator_id)
        cursor.execute(f"UPDATE dbo.iot_refrigerators SET {','.join(assignments)} WHERE id=?", *params)
    return {"id": refrigerator_id, **fields}


@app.delete("/api/iot/admin/refrigerators/{refrigerator_id}")
def deactivate_refrigerator(refrigerator_id: int, _: dict = Depends(require_admin)):
    with connection() as cn:
        cursor = cn.cursor()
        cursor.execute("UPDATE dbo.iot_refrigerators SET active=0,updated_at=SYSUTCDATETIME() WHERE id=?", refrigerator_id)
        if cursor.rowcount == 0:
            raise HTTPException(status_code=404, detail="Nevera no encontrada.")
    return {"ok": True, "message": "Nevera desactivada; se conserva su historial de auditoría."}


@app.get("/api/iot/admin/sensors")
def list_sensors(_: dict = Depends(require_admin)):
    with connection() as cn:
        cursor = cn.cursor()
        cursor.execute("SELECT s.id,s.device_id deviceId,s.refrigerator_id refrigeratorId,r.name refrigerator,s.sensor_type sensorType,s.status,s.installed_at installedAt,s.last_seen lastSeen,s.simulated FROM dbo.iot_sensors s JOIN dbo.iot_refrigerators r ON r.id=s.refrigerator_id ORDER BY s.id")
        return _rows(cursor)


@app.post("/api/iot/admin/sensors", status_code=201)
def create_sensor(body: SensorInput, _: dict = Depends(require_admin)):
    token = secrets.token_urlsafe(36)
    digest = hashlib.sha256(token.encode("utf-8")).hexdigest()
    with connection() as cn:
        cursor = cn.cursor()
        cursor.execute("INSERT INTO dbo.iot_sensors(device_id,refrigerator_id,sensor_type,status,token_hash,simulated) OUTPUT INSERTED.id VALUES(?,?,?,'ACTIVE',?,0)",
                       body.device_id, body.refrigerator_id, body.sensor_type, digest)
        sensor_id = int(cursor.fetchone()[0])
    return {"id": sensor_id, "deviceId": body.device_id, "refrigeratorId": body.refrigerator_id,
            "sensorType": body.sensor_type, "status": "ACTIVE", "deviceToken": token,
            "tokenWarning": "Guarda esta credencial ahora; no se vuelve a mostrar."}


@app.delete("/api/iot/admin/sensors/{sensor_id}")
def deactivate_sensor(sensor_id: int, _: dict = Depends(require_admin)):
    with connection() as cn:
        cursor = cn.cursor()
        cursor.execute("UPDATE dbo.iot_sensors SET status='INACTIVE' WHERE id=?", sensor_id)
        if cursor.rowcount == 0:
            raise HTTPException(status_code=404, detail="Sensor no encontrado.")
    return {"id": sensor_id, "status": "INACTIVE", "message": "Sensor desactivado; se conserva el historial."}


@app.post("/api/iot/admin/sensors/{sensor_id}/rotate-key")
def rotate_sensor_key(sensor_id: int, _: dict = Depends(require_admin)):
    token = secrets.token_urlsafe(36)
    digest = hashlib.sha256(token.encode("utf-8")).hexdigest()
    with connection() as cn:
        cursor = cn.cursor()
        cursor.execute("UPDATE dbo.iot_sensors SET token_hash=? WHERE id=? AND simulated=0", digest, sensor_id)
        if cursor.rowcount == 0:
            raise HTTPException(status_code=404, detail="Sensor físico no encontrado.")
    return {"deviceToken": token, "tokenWarning": "Guarda esta credencial ahora; no se vuelve a mostrar."}


@app.post("/api/iot/readings", status_code=201)
async def receive_sensor_reading(body: SensorReading, credentials: HTTPAuthorizationCredentials | None = Depends(bearer)):
    if not credentials:
        raise HTTPException(status_code=401, detail="Falta la credencial del sensor.")
    with connection() as cn:
        sensor = _device_sensor(body.device_id, credentials.credentials, cn)
        if sensor.simulated:
            raise HTTPException(status_code=403, detail="Los sensores simulados no aceptan lecturas físicas.")
    result = await asyncio.to_thread(_save_reading, body, "DEVICE", False)
    await _broadcast({"type": "reading", "data": result})
    if result["alert"]:
        await _broadcast({"type": "alert", "data": result["alert"]})
    return result


@app.get("/api/iot/admin/readings")
def list_readings(refrigerator_id: int | None = None, sensor_id: int | None = None,
                  start: datetime | None = None, end: datetime | None = None,
                  limit: int = Query(default=500, ge=1, le=5000), _: dict = Depends(require_admin)):
    sql = "SELECT TOP (?) rd.id,rd.sensor_id sensorId,s.device_id deviceId,s.refrigerator_id refrigeratorId,r.name refrigerator,rd.temperature,rd.humidity,rd.observed_at timestamp,rd.received_at receivedAt,rd.source,r.min_temp minTemp,r.max_temp maxTemp FROM dbo.iot_temperature_readings rd JOIN dbo.iot_sensors s ON s.id=rd.sensor_id JOIN dbo.iot_refrigerators r ON r.id=s.refrigerator_id WHERE 1=1"
    params: list[Any] = [limit]
    if refrigerator_id:
        sql += " AND r.id=?"; params.append(refrigerator_id)
    if sensor_id:
        sql += " AND s.id=?"; params.append(sensor_id)
    if start:
        sql += " AND rd.observed_at>=?"; params.append(start.replace(tzinfo=timezone.utc).replace(tzinfo=None))
    if end:
        sql += " AND rd.observed_at<=?"; params.append(end.replace(tzinfo=timezone.utc).replace(tzinfo=None))
    sql += " ORDER BY rd.observed_at DESC,rd.id DESC"
    with connection() as cn:
        cursor = cn.cursor(); cursor.execute(sql, *params)
        return _rows(cursor)


@app.get("/api/iot/admin/readings/export")
def export_readings(refrigerator_id: int | None = None, start: datetime | None = None,
                    end: datetime | None = None, format: Literal["xlsx", "pdf"] = "xlsx",
                    _: dict = Depends(require_admin)):
    rows = list_readings(refrigerator_id, None, start, end, 5000, {})
    headers = ["Fecha UTC", "Nevera", "Sensor", "Temperatura °C", "Humedad %", "Origen"]
    values = [[row.get("timestamp"), row.get("refrigerator"), row.get("deviceId"),
               row.get("temperature"), row.get("humidity"), row.get("source")] for row in rows]
    content = BytesIO()
    if format == "xlsx":
        from openpyxl import Workbook
        from openpyxl.styles import Font, PatternFill
        workbook = Workbook()
        sheet = workbook.active
        sheet.title = "Mediciones IoT"
        sheet.append(headers)
        for cell in sheet[1]:
            cell.font = Font(bold=True, color="FFFFFF")
            cell.fill = PatternFill("solid", fgColor="087F8C")
        for row in values:
            sheet.append(row)
        sheet.freeze_panes = "A2"
        sheet.auto_filter.ref = sheet.dimensions
        for column in sheet.columns:
            sheet.column_dimensions[column[0].column_letter].width = min(35, max(16, max(len(str(cell.value or "")) for cell in column) + 2))
        workbook.save(content)
        content_type = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
        filename = "vetsphere-mediciones-cadena-frio.xlsx"
    else:
        from reportlab.lib import colors
        from reportlab.lib.pagesizes import A4, landscape
        from reportlab.lib.enums import TA_CENTER
        from reportlab.lib.styles import ParagraphStyle
        from reportlab.lib.units import mm
        from reportlab.platypus import LongTable, Paragraph, SimpleDocTemplate, Spacer, Table, TableStyle
        from xml.sax.saxutils import escape

        navy = colors.HexColor("#142b3b")
        teal = colors.HexColor("#087f8c")
        pale = colors.HexColor("#eff8fa")
        line = colors.HexColor("#d7e4ea")
        muted = colors.HexColor("#526779")
        green = colors.HexColor("#e9f8f1")
        green_text = colors.HexColor("#08744c")
        red = colors.HexColor("#fff0f0")
        red_text = colors.HexColor("#b42332")

        styles = {
            "brand": ParagraphStyle("Brand", fontName="Helvetica-Bold", fontSize=17, leading=20, textColor=colors.white),
            "subtitle": ParagraphStyle("Subtitle", fontName="Helvetica", fontSize=8.5, leading=12, textColor=colors.HexColor("#d8eef1")),
            "tag": ParagraphStyle("Tag", fontName="Helvetica-Bold", fontSize=8, leading=11, alignment=TA_CENTER, textColor=teal),
            "section": ParagraphStyle("Section", fontName="Helvetica-Bold", fontSize=10, leading=13, textColor=navy),
            "body": ParagraphStyle("Body", fontName="Helvetica", fontSize=8, leading=11, textColor=muted),
            "card_label": ParagraphStyle("CardLabel", fontName="Helvetica-Bold", fontSize=7, leading=9, textColor=muted),
            "card_value": ParagraphStyle("CardValue", fontName="Helvetica-Bold", fontSize=15, leading=18, textColor=navy),
            "table_head": ParagraphStyle("TableHead", fontName="Helvetica-Bold", fontSize=7, leading=9, textColor=colors.white),
            "table_cell": ParagraphStyle("TableCell", fontName="Helvetica", fontSize=7.2, leading=9, textColor=navy),
            "table_small": ParagraphStyle("TableSmall", fontName="Helvetica", fontSize=6.6, leading=8, textColor=muted),
            "status_ok": ParagraphStyle("StatusOk", fontName="Helvetica-Bold", fontSize=6.6, leading=8, textColor=green_text, alignment=TA_CENTER),
            "status_bad": ParagraphStyle("StatusBad", fontName="Helvetica-Bold", fontSize=6.6, leading=8, textColor=red_text, alignment=TA_CENTER),
            "empty": ParagraphStyle("Empty", fontName="Helvetica", fontSize=9, leading=13, textColor=muted, alignment=TA_CENTER),
        }

        def safe(value: Any, fallback: str = "-") -> str:
            if value is None or str(value).strip() == "":
                return fallback
            return escape(str(value))

        def formatted_timestamp(value: Any) -> str:
            if value is None:
                return "Sin fecha"
            try:
                parsed = value if isinstance(value, datetime) else datetime.fromisoformat(str(value).replace("Z", "+00:00"))
                if parsed.tzinfo is not None:
                    parsed = parsed.astimezone(timezone.utc).replace(tzinfo=None)
                return parsed.strftime("%d/%m/%Y %H:%M:%S")
            except (TypeError, ValueError):
                return safe(value)

        flagged = [row for row in rows if row.get("temperature") is not None
                   and (float(row["temperature"]) < float(row.get("minTemp", float("-inf")))
                        or float(row["temperature"]) > float(row.get("maxTemp", float("inf"))))]
        simulated = [row for row in rows if "SIM" in str(row.get("source", "")).upper()]
        measured = [float(row["temperature"]) for row in rows if row.get("temperature") is not None]
        avg_temp = sum(measured) / len(measured) if measured else None
        min_temp = min(measured) if measured else None
        max_temp = max(measured) if measured else None
        fridge_name = next((str(row.get("refrigerator")) for row in rows if row.get("refrigerator")), None)
        if refrigerator_id and not fridge_name:
            with connection() as cn:
                fridge = cn.cursor()
                fridge.execute("SELECT name FROM dbo.iot_refrigerators WHERE id=?", refrigerator_id)
                found = fridge.fetchone()
                fridge_name = found.name if found else f"Nevera #{refrigerator_id}"
        range_text = "Todas las neveras" if not refrigerator_id else fridge_name or f"Nevera #{refrigerator_id}"
        period = "Todas las fechas"
        if start or end:
            period = f"{start.strftime('%d/%m/%Y') if start else 'Inicio'} - {end.strftime('%d/%m/%Y') if end else 'Actualidad'}"
        origin_label = ("SIN MEDICIONES" if not rows else
                        "DATOS SIMULADOS" if len(simulated) == len(rows) else
                        "INCLUYE SIMULACIÓN" if simulated else "SENSORES FÍSICOS")
        generated_at = datetime.now(timezone.utc).strftime("%d/%m/%Y %H:%M UTC")

        document = SimpleDocTemplate(content, pagesize=landscape(A4), title="VetSphere | Trazabilidad de cadena de frío",
                                     author="VetSphere", subject="Registro de mediciones y conservación de medicamentos",
                                     leftMargin=13 * mm, rightMargin=13 * mm, topMargin=12 * mm, bottomMargin=17 * mm)
        width = document.width
        story: list[Any] = []

        banner = Table([[Paragraph("VetSphere", styles["brand"]),
                         Paragraph("CADENA DE FRÍO · REPORTE DE TRAZABILIDAD<br/><font size='8'>Temperatura, sensores y conservación de medicamentos</font>", styles["subtitle"]),
                         Paragraph(origin_label, styles["tag"])]], colWidths=[width * .25, width * .51, width * .24])
        banner.setStyle(TableStyle([
            ("BACKGROUND", (0, 0), (-1, -1), navy), ("BACKGROUND", (2, 0), (2, 0), colors.HexColor("#dff5f4")),
            ("VALIGN", (0, 0), (-1, -1), "MIDDLE"), ("LEFTPADDING", (0, 0), (-1, -1), 12),
            ("RIGHTPADDING", (0, 0), (-1, -1), 12), ("TOPPADDING", (0, 0), (-1, -1), 11),
            ("BOTTOMPADDING", (0, 0), (-1, -1), 11),
        ]))
        story.extend([banner, Spacer(1, 8)])
        story.append(Paragraph("RESUMEN DEL PERÍODO", styles["section"]))
        story.append(Paragraph(f"<b>Nevera:</b> {safe(range_text)} &nbsp;&nbsp;&nbsp; <b>Fechas:</b> {safe(period)} &nbsp;&nbsp;&nbsp; <b>Generado:</b> {generated_at}", styles["body"]))
        story.append(Spacer(1, 8))

        def metric(label: str, value: str, tint: Any = pale, value_color: Any = navy) -> Table:
            value_style = ParagraphStyle(f"Metric{label}", parent=styles["card_value"], textColor=value_color)
            tile = Table([[Paragraph(label.upper(), styles["card_label"])], [Paragraph(value, value_style)]], colWidths=[width / 4 - 8])
            tile.setStyle(TableStyle([("BACKGROUND", (0, 0), (-1, -1), tint), ("BOX", (0, 0), (-1, -1), .6, line),
                                      ("LEFTPADDING", (0, 0), (-1, -1), 9),
                                      ("RIGHTPADDING", (0, 0), (-1, -1), 7), ("TOPPADDING", (0, 0), (-1, -1), 7),
                                      ("BOTTOMPADDING", (0, 0), (-1, -1), 7)]))
            return tile

        metrics = Table([[metric("Mediciones", f"{len(rows):,}"), metric("En rango", f"{len(rows) - len(flagged):,}", green, green_text),
                          metric("Fuera de rango", f"{len(flagged):,}", red if flagged else pale, red_text if flagged else navy),
                          metric("Simuladas", f"{len(simulated):,}")]], colWidths=[width / 4] * 4)
        metrics.setStyle(TableStyle([("VALIGN", (0, 0), (-1, -1), "TOP"), ("LEFTPADDING", (0, 0), (-1, -1), 0),
                                     ("RIGHTPADDING", (0, 0), (-1, -1), 4), ("TOPPADDING", (0, 0), (-1, -1), 0),
                                     ("BOTTOMPADDING", (0, 0), (-1, -1), 0)]))
        story.extend([metrics, Spacer(1, 7)])
        temp_summary = "Temperatura: sin datos"
        if measured:
            temp_summary = f"Temperatura mínima: {min_temp:.2f} °C &nbsp;&nbsp; Máxima: {max_temp:.2f} °C &nbsp;&nbsp; Promedio: {avg_temp:.2f} °C"
        story.append(Paragraph(temp_summary, styles["body"]))
        story.extend([Spacer(1, 11), Paragraph("DETALLE DE MEDICIONES", styles["section"]), Spacer(1, 5)])

        table_headers = ["FECHA Y HORA (UTC)", "NEVERA", "SENSOR", "TEMP.", "RANGO PERMITIDO", "HUMEDAD", "ESTADO", "ORIGEN"]
        table_data: list[list[Any]] = [[Paragraph(label, styles["table_head"]) for label in table_headers]]
        status_rows: list[tuple[int, bool]] = []
        for row_index, row in enumerate(rows, start=1):
            temp = float(row["temperature"]) if row.get("temperature") is not None else None
            low, high = row.get("minTemp"), row.get("maxTemp")
            out_of_range = temp is not None and low is not None and high is not None and (temp < float(low) or temp > float(high))
            status_rows.append((row_index, out_of_range))
            source = str(row.get("source") or "Desconocido").upper()
            source_text = "SIMULADA" if "SIM" in source else ("SENSOR REAL" if source in {"DEVICE", "REAL", "PHYSICAL"} else source)
            table_data.append([
                Paragraph(safe(formatted_timestamp(row.get("timestamp"))), styles["table_cell"]),
                Paragraph(safe(row.get("refrigerator")), styles["table_cell"]),
                Paragraph(safe(row.get("deviceId")), styles["table_small"]),
                Paragraph("Sin dato" if temp is None else f"{temp:.2f} °C", styles["table_cell"]),
                Paragraph(f"{float(low):.1f} - {float(high):.1f} °C" if low is not None and high is not None else "Sin configurar", styles["table_cell"]),
                Paragraph("Sin dato" if row.get("humidity") is None else f"{float(row['humidity']):.0f}%", styles["table_cell"]),
                Paragraph("FUERA DE RANGO" if out_of_range else "EN RANGO", styles["status_bad"] if out_of_range else styles["status_ok"]),
                Paragraph(safe(source_text), styles["table_small"]),
            ])
        if not rows:
            story.append(Paragraph("No hay mediciones para los filtros seleccionados.", styles["empty"]))
        else:
            widths = [92, 116, 83, 65, 95, 65, 87, 78]
            scale = width / sum(widths)
            detail = LongTable(table_data, colWidths=[item * scale for item in widths], repeatRows=1, hAlign="LEFT")
            table_style = [
                ("BACKGROUND", (0, 0), (-1, 0), teal), ("VALIGN", (0, 0), (-1, -1), "MIDDLE"),
                ("GRID", (0, 0), (-1, -1), .35, line), ("ROWBACKGROUNDS", (0, 1), (-1, -1), [colors.white, pale]),
                ("LEFTPADDING", (0, 0), (-1, -1), 6), ("RIGHTPADDING", (0, 0), (-1, -1), 6),
                ("TOPPADDING", (0, 0), (-1, -1), 5), ("BOTTOMPADDING", (0, 0), (-1, -1), 5),
            ]
            for index, out_of_range in status_rows:
                table_style.append(("BACKGROUND", (6, index), (6, index), red if out_of_range else green))
                if out_of_range:
                    table_style.append(("BACKGROUND", (3, index), (3, index), red))
            detail.setStyle(TableStyle(table_style))
            story.append(detail)

        def draw_footer(canvas: Any, _document: Any) -> None:
            canvas.saveState()
            canvas.setStrokeColor(line)
            canvas.setLineWidth(.6)
            canvas.line(document.leftMargin, 11 * mm, landscape(A4)[0] - document.rightMargin, 11 * mm)
            canvas.setFont("Helvetica", 7)
            canvas.setFillColor(muted)
            canvas.drawString(document.leftMargin, 6.5 * mm, f"VetSphere | Trazabilidad de medicamentos | Generado {generated_at}")
            canvas.drawRightString(landscape(A4)[0] - document.rightMargin, 6.5 * mm, f"Página {canvas.getPageNumber()}")
            canvas.restoreState()

        document.build(story, onFirstPage=draw_footer, onLaterPages=draw_footer)
        content_type = "application/pdf"
        filename = "vetsphere-mediciones-cadena-frio.pdf"
    content.seek(0)
    return StreamingResponse(content, media_type=content_type,
                             headers={"Content-Disposition": f'attachment; filename="{filename}"'})


@app.get("/api/iot/admin/statistics")
def reading_statistics(refrigerator_id: int | None = None, hours: int = Query(default=24, ge=1, le=720), _: dict = Depends(require_admin)):
    sql = "SELECT MIN(rd.temperature) minimum,MAX(rd.temperature) maximum,AVG(CAST(rd.temperature AS FLOAT)) average,COUNT(*) measurementCount,SUM(CASE WHEN rd.temperature<r.min_temp OR rd.temperature>r.max_temp THEN 1 ELSE 0 END) outOfRangeCount FROM dbo.iot_temperature_readings rd JOIN dbo.iot_sensors s ON s.id=rd.sensor_id JOIN dbo.iot_refrigerators r ON r.id=s.refrigerator_id WHERE rd.observed_at>=DATEADD(hour,?,SYSUTCDATETIME())"
    params: list[Any] = [-hours]
    if refrigerator_id:
        sql += " AND r.id=?"; params.append(refrigerator_id)
    with connection() as cn:
        cursor = cn.cursor(); cursor.execute(sql, *params)
        return _to_dict(cursor, cursor.fetchone())


@app.get("/api/iot/admin/alerts")
def list_alerts(status: str | None = None, limit: int = Query(default=300, ge=1, le=1000), _: dict = Depends(require_admin)):
    sql = "SELECT TOP (?) a.id,a.refrigerator_id refrigeratorId,r.name refrigerator,a.sensor_id sensorId,s.device_id deviceId,a.reading_id readingId,a.alert_type type,a.severity,a.status,a.message,a.temperature,a.created_at createdAt,a.updated_at updatedAt,a.attended_by attendedBy,a.resolved_at resolvedAt FROM dbo.iot_alerts a JOIN dbo.iot_refrigerators r ON r.id=a.refrigerator_id LEFT JOIN dbo.iot_sensors s ON s.id=a.sensor_id WHERE 1=1"
    params: list[Any] = [limit]
    if status:
        sql += " AND a.status=?"; params.append(status.upper())
    sql += " ORDER BY a.created_at DESC,a.id DESC"
    with connection() as cn:
        cursor = cn.cursor(); cursor.execute(sql, *params)
        return _rows(cursor)


@app.post("/api/iot/admin/alerts/{alert_id}/actions")
def manage_alert(alert_id: int, body: AlertActionInput, user: dict = Depends(require_admin)):
    email = str(user.get("sub", "administrador"))
    now = datetime.now(timezone.utc).replace(tzinfo=None)
    with connection() as cn:
        cursor = cn.cursor()
        cursor.execute("SELECT status FROM dbo.iot_alerts WHERE id=?", alert_id)
        if not cursor.fetchone():
            raise HTTPException(status_code=404, detail="Alerta no encontrada.")
        resolved = now if body.status == "RESOLVED" else None
        cursor.execute("UPDATE dbo.iot_alerts SET status=?,attended_by=?,resolved_at=?,updated_at=? WHERE id=?",
                       body.status, email, resolved, now, alert_id)
        cursor.execute("INSERT INTO dbo.iot_alert_actions(alert_id,action,note,user_email) VALUES(?,?,?,?)",
                       alert_id, body.status, body.note, email)
    response = {"id": alert_id, "status": body.status, "attendedBy": email, "note": body.note}
    awaitable = _broadcast({"type": "alert", "data": response})
    if main_loop:
        asyncio.run_coroutine_threadsafe(awaitable, main_loop)
    return response


@app.get("/api/iot/admin/alerts/{alert_id}/actions")
def list_alert_actions(alert_id: int, _: dict = Depends(require_admin)):
    with connection() as cn:
        cursor = cn.cursor()
        cursor.execute("SELECT id,action,note,user_email userEmail,created_at createdAt FROM dbo.iot_alert_actions WHERE alert_id=? ORDER BY created_at,id", alert_id)
        return _rows(cursor)


@app.get("/api/iot/admin/storage")
def list_storage(_: dict = Depends(require_admin)):
    with connection() as cn:
        cursor = cn.cursor()
        cursor.execute("SELECT ps.id,ps.product_code productCode,ps.refrigerator_id refrigeratorId,r.name refrigerator,ps.review_required reviewRequired,ps.last_review_alert_id lastReviewAlertId,ps.updated_at updatedAt FROM dbo.iot_product_storage ps JOIN dbo.iot_refrigerators r ON r.id=ps.refrigerator_id ORDER BY ps.product_code")
        return _rows(cursor)


@app.post("/api/iot/admin/storage", status_code=201)
def assign_product(body: StorageInput, _: dict = Depends(require_admin)):
    with connection() as cn:
        cursor = cn.cursor()
        cursor.execute("MERGE dbo.iot_product_storage AS target USING (SELECT ? product_code,? refrigerator_id) AS source ON target.product_code=source.product_code AND target.refrigerator_id=source.refrigerator_id WHEN MATCHED THEN UPDATE SET updated_at=SYSUTCDATETIME() WHEN NOT MATCHED THEN INSERT(product_code,refrigerator_id) VALUES(source.product_code,source.refrigerator_id);",
                       body.product_code, body.refrigerator_id)
    return {**body.model_dump(), "reviewRequired": False}


@app.patch("/api/iot/admin/storage/{storage_id}/review")
def review_product(storage_id: int, body: StorageReviewInput, user: dict = Depends(require_admin)):
    with connection() as cn:
        cursor = cn.cursor()
        cursor.execute("SELECT last_review_alert_id FROM dbo.iot_product_storage WHERE id=?", storage_id)
        row = cursor.fetchone()
        if not row:
            raise HTTPException(status_code=404, detail="Asociación de producto no encontrada.")
        cursor.execute("UPDATE dbo.iot_product_storage SET review_required=?,updated_at=SYSUTCDATETIME() WHERE id=?", body.review_required, storage_id)
        if row.last_review_alert_id:
            cursor.execute("INSERT INTO dbo.iot_alert_actions(alert_id,action,note,user_email) VALUES(?,?,?,?)",
                           row.last_review_alert_id, "PRODUCT_REVIEW", body.note, str(user.get("sub", "administrador")))
    return {"id": storage_id, "reviewRequired": body.review_required}


@app.get("/api/iot/admin/simulation")
def simulation_status(_: dict = Depends(require_admin)):
    return simulation.status()


@app.post("/api/iot/admin/simulation/start")
async def start_simulation(body: SimulationSettings, _: dict = Depends(require_admin)):
    return await simulation.start(body.interval_seconds, body.mode)


@app.post("/api/iot/admin/simulation/stop")
async def stop_simulation(_: dict = Depends(require_admin)):
    return await simulation.stop()


@app.post("/api/iot/admin/simulation/restart")
async def restart_simulation(body: SimulationSettings, _: dict = Depends(require_admin)):
    def reconnect():
        with connection() as cn:
            cn.cursor().execute("UPDATE dbo.iot_sensors SET status='ACTIVE' WHERE simulated=1")
    return await simulation.restart(body.interval_seconds, body.mode, reconnect)


@app.post("/api/iot/admin/simulation/force-critical/{refrigerator_id}")
async def force_critical(refrigerator_id: int, _: dict = Depends(require_admin)):
    with connection() as cn:
        cursor = cn.cursor()
        cursor.execute("SELECT TOP 1 s.device_id,r.min_temp,r.max_temp FROM dbo.iot_sensors s JOIN dbo.iot_refrigerators r ON r.id=s.refrigerator_id WHERE r.id=? AND s.simulated=1 AND s.status='ACTIVE'", refrigerator_id)
        sensor = cursor.fetchone()
    if not sensor:
        raise HTTPException(status_code=404, detail="No hay un sensor SIMULADO activo en esa nevera.")
    return await simulation.force_critical(sensor.device_id, float(sensor.max_temp))


@app.post("/api/iot/admin/simulation/sensors/{sensor_id}/disconnect")
async def disconnect_simulated_sensor(sensor_id: int, user: dict = Depends(require_admin)):
    result = await asyncio.to_thread(_set_sensor_status, sensor_id, "DISCONNECTED", str(user.get("sub", "")))
    await _broadcast({"type": "sensor", "data": result})
    if result["alert"]:
        await _broadcast({"type": "alert", "data": result["alert"]})
    return result


@app.post("/api/iot/admin/simulation/sensors/{sensor_id}/reconnect")
async def reconnect_simulated_sensor(sensor_id: int, user: dict = Depends(require_admin)):
    result = await asyncio.to_thread(_set_sensor_status, sensor_id, "ACTIVE", str(user.get("sub", "")))
    await _broadcast({"type": "sensor", "data": result})
    return result


@app.websocket("/api/iot/ws")
async def dashboard_socket(socket: WebSocket):
    # Los navegadores no permiten agregar Authorization a WebSocket. Se usa un
    # subprotocolo privado para no filtrar el JWT en URLs, historial o access logs.
    protocols = [value.strip() for value in socket.headers.get("sec-websocket-protocol", "").split(",")]
    token = next((value.removeprefix("vetsphere.jwt.") for value in protocols
                  if value.startswith("vetsphere.jwt.")), "")
    try:
        await asyncio.to_thread(_validate_admin_token, token)
    except HTTPException:
        await socket.close(code=1008, reason="Autenticación de administrador requerida")
        return
    await socket.accept(subprotocol="vetsphere")
    websockets.add(socket)
    try:
        await socket.send_json({"type": "connected", "data": {"message": "Conexión de cadena de frío activa."}})
        while True:
            await socket.receive_text()
    except WebSocketDisconnect:
        pass
    finally:
        websockets.discard(socket)

