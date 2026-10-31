#!/bin/bash
# VetSphere - inicialización de la base de datos del mini servicio SOAP
# Se ejecuta la primera vez que arranca el contenedor SQL Server.
set -e
/opt/mssql-tools18/bin/sqlcmd -S localhost -U sa -P "$MSSQL_SA_PASSWORD" -C \
  -Q "IF DB_ID('VeterinariaMiniDB') IS NULL CREATE DATABASE VeterinariaMiniDB"
echo "Base de datos VeterinariaMiniDB creada (o ya existente)."
