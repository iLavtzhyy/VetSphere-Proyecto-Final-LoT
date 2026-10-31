# Inicia SQL Server y los cuatro servicios de VetSphere en segundo plano.
$ErrorActionPreference = 'Stop'
$root = $PSScriptRoot
$logDir = Join-Path $root 'logs'
New-Item -ItemType Directory -Path $logDir -Force | Out-Null

function Test-PortListening([int]$Port) {
    return [bool](Get-NetTCPConnection -State Listen -LocalPort $Port -ErrorAction SilentlyContinue | Select-Object -First 1)
}

function Wait-Port([int]$Port, [int]$TimeoutSeconds = 180) {
    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    while ((Get-Date) -lt $deadline) {
        if (Test-PortListening $Port) { return $true }
        Start-Sleep -Seconds 2
    }
    return $false
}

function Test-ServiceReady($Service) {
    try {
        $response = Invoke-WebRequest -Uri $Service.CheckUrl -UseBasicParsing -TimeoutSec 4
        return $response.StatusCode -eq 200
    } catch {
        return $false
    }
}

function Wait-Service($Service, [int]$TimeoutSeconds = 180) {
    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    while ((Get-Date) -lt $deadline) {
        if (Test-ServiceReady $Service) { return $true }
        Start-Sleep -Seconds 3
    }
    return $false
}

Write-Host 'VetSphere: preparando el entorno...' -ForegroundColor Cyan

# Encontrar un JDK 17+ para que Maven pueda compilar y arrancar Spring Boot.
$jdkCandidates = @(
    $env:JAVA_HOME,
    (Get-ChildItem 'C:\Program Files\Eclipse Adoptium\jdk-*' -Directory -ErrorAction SilentlyContinue | Sort-Object Name -Descending | Select-Object -First 1 -ExpandProperty FullName),
    (Get-ChildItem 'C:\Program Files\Java\jdk-*' -Directory -ErrorAction SilentlyContinue | Sort-Object Name -Descending | Select-Object -First 1 -ExpandProperty FullName),
    (Get-ChildItem 'C:\Program Files\JetBrains\IntelliJ IDEA*\jbr' -Directory -ErrorAction SilentlyContinue | Select-Object -First 1 -ExpandProperty FullName)
)
$jdk = $jdkCandidates | Where-Object { $_ -and (Test-Path (Join-Path $_ 'bin\javac.exe')) } | Select-Object -First 1
if (-not $jdk) { throw 'No se encontró un JDK con javac. Instala JDK 17 o superior.' }
$env:JAVA_HOME = $jdk
$env:Path = (Join-Path $jdk 'bin') + ';' + $env:Path
Write-Host "JDK: $jdk" -ForegroundColor Green

# SQL Server Express local usa el puerto que indican los archivos .env (1433).
$sqlService = Get-Service -Name 'MSSQL$SQLEXPRESS' -ErrorAction SilentlyContinue
if ($sqlService -and $sqlService.Status -ne 'Running') {
    Write-Host 'Iniciando SQL Server Express...' -ForegroundColor Yellow
    Start-Service -Name 'MSSQL$SQLEXPRESS'
}
if (-not (Wait-Port 1433 45)) { throw 'SQL Server no está disponible en localhost:1433. Revisa el servicio MSSQL$SQLEXPRESS.' }
Write-Host 'SQL Server: localhost:1433 disponible' -ForegroundColor Green

$services = @(
    @{ Name = 'Backend'; Port = 8080; CheckUrl = 'http://localhost:8080/soap/productos?wsdl'; Script = 'Backend-Proyecto-Lenguajes-main\Backend-Proyecto-Lenguajes-main\iniciar-backend.ps1' },
    @{ Name = 'Cadena de frio IoT'; Port = 8091; CheckUrl = 'http://localhost:8091/api/iot/health'; Script = 'iot-cadena-frio\iniciar-cadena-frio.ps1' },
    @{ Name = 'Frontend'; Port = 4200; CheckUrl = 'http://localhost:4200'; Script = 'Frontend-Proyecto-Lenguajes-main\Frontend-Proyecto-Lenguajes-main\iniciar-frontend.ps1' },
    @{ Name = 'Mini SOAP'; Port = 8090; CheckUrl = 'http://localhost:8090'; Script = 'MiniProyecto-SOAP-master\MiniProyecto-SOAP-master\iniciar-soap.ps1' }
)

$powershell = (Get-Command powershell.exe -ErrorAction Stop).Source
foreach ($service in $services) {
    if (Test-ServiceReady $service) {
        Write-Host "$($service.Name): ya está listo en http://localhost:$($service.Port)" -ForegroundColor DarkYellow
        continue
    }
    if (Test-PortListening $service.Port) {
        Write-Host "$($service.Name): el puerto $($service.Port) está ocupado, pero el servicio no respondió" -ForegroundColor Red
        continue
    }
    $scriptPath = Join-Path $root $service.Script
    if (-not (Test-Path $scriptPath)) { throw "No se encontró el iniciador de $($service.Name): $scriptPath" }
    $stdout = Join-Path $logDir "$($service.Name -replace ' ', '-').out.log"
    $stderr = Join-Path $logDir "$($service.Name -replace ' ', '-').err.log"
    Start-Process -FilePath $powershell -ArgumentList @('-NoProfile', '-ExecutionPolicy', 'Bypass', '-File', "`"$scriptPath`"") -WorkingDirectory (Split-Path $scriptPath) -RedirectStandardOutput $stdout -RedirectStandardError $stderr -WindowStyle Hidden | Out-Null
    Write-Host "$($service.Name): iniciando en localhost:$($service.Port)..." -ForegroundColor Yellow
}

$allReady = $true
foreach ($service in $services) {
    if (Test-ServiceReady $service) {
        Write-Host "$($service.Name): listo en http://localhost:$($service.Port)" -ForegroundColor Green
    } elseif (Wait-Service $service 180) {
        Write-Host "$($service.Name): listo en http://localhost:$($service.Port)" -ForegroundColor Green
    } else {
        $allReady = $false
        Write-Host "$($service.Name): no abrió el puerto $($service.Port). Revisa logs\$($service.Name -replace ' ', '-').err.log y .out.log" -ForegroundColor Red
    }
}

Write-Host ''
Write-Host 'Accesos de VetSphere:' -ForegroundColor Cyan
Write-Host '  Aplicación web: http://localhost:4200'
Write-Host '  API REST:       http://localhost:8080/api'
Write-Host '  SOAP WSDL:      http://localhost:8080/soap/productos?wsdl'
Write-Host '  Mini SOAP:      http://localhost:8090'
Write-Host '  SQL Server:     localhost:1433'
Write-Host "  Registros:      $logDir"
if (-not $allReady) { exit 1 }
