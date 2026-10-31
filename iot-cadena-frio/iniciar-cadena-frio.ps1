$ErrorActionPreference = 'Stop'
Set-Location -Path $PSScriptRoot

$python = Join-Path $PSScriptRoot '.venv\Scripts\python.exe'
if (-not (Test-Path $python)) {
    $launcher = Get-Command py.exe -ErrorAction SilentlyContinue
    if ($launcher) { & $launcher.Source -3 -m venv (Join-Path $PSScriptRoot '.venv') }
    else { python -m venv (Join-Path $PSScriptRoot '.venv') }
}
if (-not (Test-Path $python)) { throw 'No se pudo crear el entorno Python 3.10+ del servicio IoT.' }

& $python -m pip install --disable-pip-version-check -r (Join-Path $PSScriptRoot 'requirements.txt')
if ($LASTEXITCODE -ne 0) { throw 'No se pudieron instalar las dependencias del servicio IoT.' }

$port = if ($env:IOT_PORT) { $env:IOT_PORT } else { '8091' }
Write-Host "VetSphere Cadena de frío IoT: http://localhost:$port" -ForegroundColor Cyan
& $python -m uvicorn app.main:app --host 0.0.0.0 --port $port
