# Script de inicio rápido para pruebas locales
# Fleet Management - Local Testing

Write-Host "=========================================" -ForegroundColor Cyan
Write-Host "  Fleet Management - Inicio Local" -ForegroundColor Cyan
Write-Host "=========================================" -ForegroundColor Cyan
Write-Host ""

# Verificar Docker
Write-Host "1. Verificando Docker..." -ForegroundColor Yellow
$dockerVersion = docker --version 2>$null
if ($LASTEXITCODE -ne 0) {
    Write-Host "❌ Docker no está instalado o no está en el PATH" -ForegroundColor Red
    Write-Host "   Instala Docker Desktop desde: https://www.docker.com/products/docker-desktop" -ForegroundColor Red
    exit 1
}
Write-Host "   ✅ Docker encontrado: $dockerVersion" -ForegroundColor Green

# Verificar Docker Compose
Write-Host ""
Write-Host "2. Verificando Docker Compose..." -ForegroundColor Yellow
$composeVersion = docker-compose --version 2>$null
if ($LASTEXITCODE -ne 0) {
    Write-Host "❌ Docker Compose no está disponible" -ForegroundColor Red
    exit 1
}
Write-Host "   ✅ Docker Compose encontrado: $composeVersion" -ForegroundColor Green

# Verificar puertos disponibles
Write-Host ""
Write-Host "3. Verificando puertos disponibles..." -ForegroundColor Yellow
$ports = @(5432, 4566, 8080)
$portsInUse = @()

foreach ($port in $ports) {
    $connection = netstat -ano | Select-String ":$port " | Select-Object -First 1
    if ($connection) {
        $portsInUse += $port
        Write-Host "   ⚠️  Puerto $port está en uso" -ForegroundColor Yellow
    } else {
        Write-Host "   ✅ Puerto $port disponible" -ForegroundColor Green
    }
}

if ($portsInUse.Count -gt 0) {
    Write-Host ""
    Write-Host "⚠️  Advertencia: Algunos puertos están en uso" -ForegroundColor Yellow
    Write-Host "   Puertos ocupados: $($portsInUse -join ', ')" -ForegroundColor Yellow
    Write-Host ""
    $continue = Read-Host "¿Deseas continuar de todos modos? (s/n)"
    if ($continue -ne 's' -and $continue -ne 'S') {
        Write-Host "Operación cancelada" -ForegroundColor Red
        exit 1
    }
}

# Limpiar contenedores anteriores
Write-Host ""
Write-Host "4. Limpiando contenedores anteriores..." -ForegroundColor Yellow
docker-compose down -v 2>$null
Write-Host "   ✅ Limpieza completada" -ForegroundColor Green

# Construir y levantar servicios
Write-Host ""
Write-Host "5. Construyendo y levantando servicios..." -ForegroundColor Yellow
Write-Host "   (Esto puede tomar 3-5 minutos la primera vez)" -ForegroundColor Cyan
Write-Host ""

docker-compose up --build -d

if ($LASTEXITCODE -ne 0) {
    Write-Host ""
    Write-Host "❌ Error al levantar los servicios" -ForegroundColor Red
    Write-Host "   Revisa los logs con: docker-compose logs" -ForegroundColor Yellow
    exit 1
}

# Esperar a que los servicios estén listos
Write-Host ""
Write-Host "6. Esperando a que los servicios estén listos..." -ForegroundColor Yellow
Write-Host "   Esto puede tomar 30-60 segundos..." -ForegroundColor Cyan

$maxAttempts = 30
$attempt = 0
$allHealthy = $false

while ($attempt -lt $maxAttempts -and -not $allHealthy) {
    Start-Sleep -Seconds 2
    $attempt++
    
    $status = docker-compose ps --format json | ConvertFrom-Json
    $healthy = ($status | Where-Object { $_.Health -eq "healthy" -or $_.State -eq "running" }).Count
    $total = $status.Count
    
    Write-Host "   Intento $attempt/$maxAttempts - Servicios listos: $healthy/$total" -ForegroundColor Cyan
    
    if ($healthy -eq $total) {
        $allHealthy = $true
    }
}

if (-not $allHealthy) {
    Write-Host ""
    Write-Host "⚠️  Algunos servicios pueden no estar completamente listos" -ForegroundColor Yellow
    Write-Host "   Revisa el estado con: docker-compose ps" -ForegroundColor Yellow
}

# Verificar estado final
Write-Host ""
Write-Host "7. Estado de los servicios:" -ForegroundColor Yellow
docker-compose ps

# Probar conectividad
Write-Host ""
Write-Host "8. Probando conectividad de la API..." -ForegroundColor Yellow
Start-Sleep -Seconds 5

try {
    $response = Invoke-WebRequest -Uri "http://localhost:8080/api/v1/solicitudes" -Method GET -TimeoutSec 10 -ErrorAction Stop
    Write-Host "   ✅ API respondiendo correctamente (Status: $($response.StatusCode))" -ForegroundColor Green
} catch {
    Write-Host "   ⚠️  API aún no está lista o hay un error" -ForegroundColor Yellow
    Write-Host "   Espera unos segundos más y verifica con: curl http://localhost:8080/api/v1/solicitudes" -ForegroundColor Yellow
}

# Resumen
Write-Host ""
Write-Host "=========================================" -ForegroundColor Cyan
Write-Host "  ✅ Entorno Local Iniciado" -ForegroundColor Green
Write-Host "=========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "Servicios disponibles:" -ForegroundColor White
Write-Host "  📦 PostgreSQL:    http://localhost:5432" -ForegroundColor White
Write-Host "  ☁️  LocalStack:    http://localhost:4566" -ForegroundColor White
Write-Host "  🚀 API REST:      http://localhost:8080" -ForegroundColor White
Write-Host ""
Write-Host "Comandos útiles:" -ForegroundColor White
Write-Host "  Ver logs:         docker-compose logs -f" -ForegroundColor Cyan
Write-Host "  Ver logs app:     docker-compose logs -f app" -ForegroundColor Cyan
Write-Host "  Detener:          docker-compose down" -ForegroundColor Cyan
Write-Host "  Reiniciar:        docker-compose restart" -ForegroundColor Cyan
Write-Host ""
Write-Host "Prueba la API:" -ForegroundColor White
Write-Host "  curl http://localhost:8080/api/v1/solicitudes" -ForegroundColor Cyan
Write-Host ""
Write-Host "📖 Guía completa: LOCAL-TESTING.md" -ForegroundColor Yellow
Write-Host ""
