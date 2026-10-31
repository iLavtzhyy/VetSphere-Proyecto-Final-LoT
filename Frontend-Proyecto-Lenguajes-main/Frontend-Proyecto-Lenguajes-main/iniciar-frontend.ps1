# Script para iniciar el Frontend Angular de VetSphere
Set-Location -Path $PSScriptRoot

Write-Host "Iniciando servidor de desarrollo Angular en http://localhost:4200..." -ForegroundColor Green
npx ng serve --configuration development --host 0.0.0.0 --port 4200
