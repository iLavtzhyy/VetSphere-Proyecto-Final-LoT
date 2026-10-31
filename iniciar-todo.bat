# Script maestro para levantar Backend, Frontend y Mini SOAP simultáneamente
$root = $PSScriptRoot

Write-Host "==================================================" -ForegroundColor Cyan
Write-Host "   Levantando Ecosistema Completo VetSphere..." -ForegroundColor Cyan
Write-Host "==================================================" -ForegroundColor Cyan

# 1. Iniciar Backend
$backendScript = Join-Path $root "Backend-Proyecto-Lenguajes-main\Backend-Proyecto-Lenguajes-main\iniciar-backend.ps1"
Write-Host "Lanzando Backend (Spring Boot :8080)..." -ForegroundColor Yellow
Start-Process powershell -ArgumentList "-NoExit", "-File", "`"$backendScript`""

# 2. Iniciar Frontend
$frontendScript = Join-Path $root "Frontend-Proyecto-Lenguajes-main\Frontend-Proyecto-Lenguajes-main\iniciar-frontend.ps1"
Write-Host "Lanzando Frontend (Angular :4200)..." -ForegroundColor Yellow
Start-Process powershell -ArgumentList "-NoExit", "-File", "`"$frontendScript`""

# 3. Iniciar Mini SOAP
$soapScript = Join-Path $root "MiniProyecto-SOAP-master\MiniProyecto-SOAP-master\iniciar-soap.ps1"
Write-Host "Lanzando Mini SOAP (Spring Boot :8090)..." -ForegroundColor Yellow
Start-Process powershell -ArgumentList "-NoExit", "-File", "`"$soapScript`""

Write-Host "¡Los 3 servicios fueron lanzados en ventanas independientes de PowerShell!" -ForegroundColor Green
Write-Host ""
Write-Host "Puntos de acceso:" -ForegroundColor Cyan
Write-Host " - Frontend Web:    http://localhost:4200"
Write-Host " - Backend REST:    http://localhost:8080/api"
Write-Host " - Backend SOAP:    http://localhost:8080/soap/productos?wsdl"
Write-Host " - Mini SOAP App:   http://localhost:8090"
