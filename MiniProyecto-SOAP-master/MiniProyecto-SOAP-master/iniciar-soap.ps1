# Script para iniciar el Mini SOAP Client en puerto 8090
Set-Location -Path $PSScriptRoot

# ── Detectar un JDK válido (JBR de IntelliJ u otro JAVA_HOME) ──────────────
$javaHomeCandidates = @(
    $env:JAVA_HOME,
    (Get-ChildItem "C:\Program Files\JetBrains\IntelliJ IDEA*\jbr" -Directory -ErrorAction SilentlyContinue | Select-Object -First 1 -ExpandProperty FullName),
    (Get-ChildItem "C:\Program Files\JetBrains\IntelliJ IDEA*\jbr" -Directory -ErrorAction SilentlyContinue | Select-Object -First 1 -ExpandProperty FullName)
)
foreach ($cand in $javaHomeCandidates) {
    if ($cand -and (Test-Path (Join-Path $cand "bin\javac.exe"))) {
        $env:JAVA_HOME = $cand
        $env:Path = (Join-Path $cand "bin") + ";" + $env:Path
        Write-Host "JAVA_HOME detectado: $cand" -ForegroundColor Cyan
        break
    }
}
if (-not (Get-Command javac -ErrorAction SilentlyContinue)) {
    Write-Host "ADVERTENCIA: no se encontró un JDK (solo JRE). Instala JDK 17+ o define JAVA_HOME." -ForegroundColor Red
}

if (Test-Path .env) {
    Write-Host "Cargando variables de entorno desde .env..." -ForegroundColor Cyan
    Get-Content .env | Where-Object { $_ -match '^\s*[^#\s]+=' } | ForEach-Object {
        $p = $_ -split '=', 2
        $key = $p[0].Trim()
        $val = $p[1].Trim()
        [System.Environment]::SetEnvironmentVariable($key, $val, "Process")
    }
}

$mvn = "C:\Program Files\JetBrains\IntelliJ IDEA 2026.2.3\plugins\maven-plugin\lib\maven3\bin\mvn.cmd"
if (-not (Test-Path $mvn)) {
    $mvn = "mvn"
}

Write-Host "Iniciando Mini SOAP en http://localhost:8090..." -ForegroundColor Green
& $mvn spring-boot:run
