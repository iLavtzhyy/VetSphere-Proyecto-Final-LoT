from __future__ import annotations

import os
import re
from contextlib import contextmanager
from pathlib import Path
from typing import Iterator

import pyodbc
from dotenv import load_dotenv


ROOT = Path(__file__).resolve().parents[2]
BACKEND = ROOT / "Backend-Proyecto-Lenguajes-main" / "Backend-Proyecto-Lenguajes-main"
load_dotenv(BACKEND / ".env", override=False)
load_dotenv(ROOT / ".env", override=False)


def _database_connection_string() -> str:
    configured = os.getenv("DB_URL", "")
    server = "localhost,1433"
    database = "VeterinariaDB"
    encrypt = "yes"
    trust_certificate = "yes"  # Perfil local: SQL Server Express suele usar un certificado propio.

    if configured:
        match = re.search(r"sqlserver://([^;]+)", configured, re.IGNORECASE)
        if match:
            server = match.group(1).replace(":", ",")
        for name, value in re.findall(r"(?:^|;)([^;=]+)=([^;]*)", configured):
            key = name.strip().lower()
            value = value.strip()
            if key == "databasename":
                database = value
            elif key == "encrypt":
                encrypt = "yes" if value.lower() in {"true", "yes", "1"} else "no"
            elif key == "trustservercertificate":
                trust_certificate = "yes" if value.lower() in {"true", "yes", "1"} else "no"

    driver = os.getenv("IOT_ODBC_DRIVER", "ODBC Driver 18 for SQL Server")
    user = os.getenv("DB_USERNAME", "")
    password = os.getenv("DB_PASSWORD", "")
    if not user or not password:
        raise RuntimeError("Configura DB_USERNAME y DB_PASSWORD en el .env del backend.")
    return (
        f"DRIVER={{{driver}}};SERVER={server};DATABASE={database};"
        f"UID={user};PWD={password};Encrypt={encrypt};"
        f"TrustServerCertificate={trust_certificate};Connection Timeout=8"
    )


def connect() -> pyodbc.Connection:
    return pyodbc.connect(_database_connection_string(), autocommit=False)


@contextmanager
def connection() -> Iterator[pyodbc.Connection]:
    cn = connect()
    try:
        yield cn
        cn.commit()
    except Exception:
        cn.rollback()
        raise
    finally:
        cn.close()


SCHEMA = [
    """IF OBJECT_ID(N'dbo.iot_refrigerators', N'U') IS NULL
       CREATE TABLE dbo.iot_refrigerators (
         id INT IDENTITY(1,1) PRIMARY KEY, code NVARCHAR(40) NOT NULL UNIQUE,
         name NVARCHAR(120) NOT NULL, location NVARCHAR(180) NOT NULL,
         capacity NVARCHAR(80) NULL, min_temp DECIMAL(5,2) NOT NULL,
         max_temp DECIMAL(5,2) NOT NULL, active BIT NOT NULL DEFAULT 1,
         created_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
         updated_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
         CONSTRAINT CK_iot_refrigerator_range CHECK (min_temp < max_temp)
       )""",
    """IF OBJECT_ID(N'dbo.iot_sensors', N'U') IS NULL
       CREATE TABLE dbo.iot_sensors (
         id INT IDENTITY(1,1) PRIMARY KEY, device_id NVARCHAR(80) NOT NULL UNIQUE,
         refrigerator_id INT NOT NULL, sensor_type NVARCHAR(80) NOT NULL,
         status NVARCHAR(20) NOT NULL DEFAULT 'ACTIVE', installed_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
         last_seen DATETIME2 NULL, token_hash CHAR(64) NULL, simulated BIT NOT NULL DEFAULT 0,
         CONSTRAINT FK_iot_sensor_refrigerator FOREIGN KEY (refrigerator_id) REFERENCES dbo.iot_refrigerators(id)
       )""",
    """IF OBJECT_ID(N'dbo.iot_temperature_readings', N'U') IS NULL
       CREATE TABLE dbo.iot_temperature_readings (
         id BIGINT IDENTITY(1,1) PRIMARY KEY, sensor_id INT NOT NULL,
         temperature DECIMAL(6,2) NOT NULL, humidity DECIMAL(6,2) NULL,
         observed_at DATETIME2 NOT NULL, received_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
         source NVARCHAR(20) NOT NULL,
         CONSTRAINT FK_iot_reading_sensor FOREIGN KEY (sensor_id) REFERENCES dbo.iot_sensors(id)
       )""",
    """IF OBJECT_ID(N'dbo.iot_alerts', N'U') IS NULL
       CREATE TABLE dbo.iot_alerts (
         id BIGINT IDENTITY(1,1) PRIMARY KEY, refrigerator_id INT NOT NULL, sensor_id INT NULL,
         reading_id BIGINT NULL, alert_type NVARCHAR(32) NOT NULL, severity NVARCHAR(16) NOT NULL,
         status NVARCHAR(16) NOT NULL DEFAULT 'PENDING', message NVARCHAR(500) NOT NULL,
         temperature DECIMAL(6,2) NULL, created_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
         updated_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(), attended_by NVARCHAR(180) NULL,
         resolved_at DATETIME2 NULL,
         CONSTRAINT FK_iot_alert_refrigerator FOREIGN KEY (refrigerator_id) REFERENCES dbo.iot_refrigerators(id),
         CONSTRAINT FK_iot_alert_sensor FOREIGN KEY (sensor_id) REFERENCES dbo.iot_sensors(id),
         CONSTRAINT FK_iot_alert_reading FOREIGN KEY (reading_id) REFERENCES dbo.iot_temperature_readings(id)
       )""",
    """IF OBJECT_ID(N'dbo.iot_alert_actions', N'U') IS NULL
       CREATE TABLE dbo.iot_alert_actions (
         id BIGINT IDENTITY(1,1) PRIMARY KEY, alert_id BIGINT NOT NULL,
         action NVARCHAR(24) NOT NULL, note NVARCHAR(1000) NOT NULL,
         user_email NVARCHAR(180) NOT NULL, created_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
         CONSTRAINT FK_iot_action_alert FOREIGN KEY (alert_id) REFERENCES dbo.iot_alerts(id)
       )""",
    """IF OBJECT_ID(N'dbo.iot_product_storage', N'U') IS NULL
       CREATE TABLE dbo.iot_product_storage (
         id BIGINT IDENTITY(1,1) PRIMARY KEY, product_code NVARCHAR(80) NOT NULL,
         refrigerator_id INT NOT NULL, review_required BIT NOT NULL DEFAULT 0,
         last_review_alert_id BIGINT NULL, updated_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
         CONSTRAINT FK_iot_storage_refrigerator FOREIGN KEY (refrigerator_id) REFERENCES dbo.iot_refrigerators(id),
         CONSTRAINT FK_iot_storage_alert FOREIGN KEY (last_review_alert_id) REFERENCES dbo.iot_alerts(id),
         CONSTRAINT UQ_iot_product_storage UNIQUE (product_code, refrigerator_id)
       )""",
    "IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name='IX_iot_reading_sensor_time') CREATE INDEX IX_iot_reading_sensor_time ON dbo.iot_temperature_readings(sensor_id, observed_at DESC)",
    "IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name='IX_iot_alert_status_time') CREATE INDEX IX_iot_alert_status_time ON dbo.iot_alerts(status, created_at DESC)",
    "IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name='IX_iot_sensor_fridge') CREATE INDEX IX_iot_sensor_fridge ON dbo.iot_sensors(refrigerator_id, status)",
]


def initialize_schema() -> None:
    with connection() as cn:
        cursor = cn.cursor()
        for statement in SCHEMA:
            cursor.execute(statement)
        examples = [
            ("SIM-FR-01", "Nevera de vacunas A", "Farmacia · Zona fría 1", "120 L", 2.0, 8.0),
            ("SIM-FR-02", "Nevera de medicamentos B", "Farmacia · Respaldo", "180 L", 2.0, 8.0),
            ("SIM-FR-03", "Nevera de biológicos C", "Clínica · Laboratorio", "90 L", 2.0, 8.0),
        ]
        for code, name, location, capacity, low, high in examples:
            cursor.execute("SELECT id FROM dbo.iot_refrigerators WHERE code=?", code)
            existing = cursor.fetchone()
            if not existing:
                cursor.execute(
                    "INSERT INTO dbo.iot_refrigerators(code,name,location,capacity,min_temp,max_temp) VALUES(?,?,?,?,?,?)",
                    code, name, location, capacity, low, high,
                )
        cursor.execute("SELECT id, code FROM dbo.iot_refrigerators WHERE code LIKE 'SIM-FR-%'")
        for refrigerator_id, code in cursor.fetchall():
            suffix = code[-2:]
            cursor.execute("SELECT COUNT(*) FROM dbo.iot_sensors WHERE refrigerator_id=? AND simulated=1", refrigerator_id)
            if cursor.fetchone()[0] == 0:
                cursor.execute(
                    "INSERT INTO dbo.iot_sensors(device_id,refrigerator_id,sensor_type,status,simulated) VALUES(?,?,?,'ACTIVE',1)",
                    f"SIM-SENSOR-{suffix}", refrigerator_id, "Simulación DS18B20",
                )

