@echo off
setlocal
title VetSphere - Inicio completo
cd /d "%~dp0"
echo ================================================
echo   INICIANDO VETSPHERE - TODOS LOS SERVICIOS
echo ================================================
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0iniciar-todo.ps1"
if errorlevel 1 (
    echo.
    echo Hubo un problema al iniciar VetSphere. Revisa los mensajes y la carpeta logs.
    pause
    exit /b 1
)
echo.
echo VetSphere quedo iniciado. Puedes cerrar esta ventana.
pause
endlocal
