# ============================================================
#  RapidoCourier - Arranque de todos los microservicios
#
#  Uso (desde PowerShell, en la raiz del proyecto):
#      .\iniciar-backend.ps1
#
#  Abre cada servicio en su propia ventana, en el orden correcto.
#  Para apagar todo: cerrar las ventanas que se abrieron.
#
#  Requisitos: Java 17+, Maven, y el repositorio de configuracion
#  en ~/rapidocourier-config (ver README, seccion Config Server).
# ============================================================

$raiz = $PSScriptRoot

# El secreto JWT no se guarda en el codigo: se genera uno nuevo en cada arranque.
# Todos los servicios de esta corrida comparten el mismo.
$jwtSecret = -join ((65..90) + (97..122) + (48..57) | Get-Random -Count 48 | ForEach-Object { [char]$_ })

# Token de RENIEC (decolecta.com): se toma de la variable de entorno o se pide.
$reniecToken = $env:RENIEC_TOKEN
if (-not $reniecToken) {
    $reniecToken = Read-Host "Pega tu token de decolecta (RENIEC) y presiona Enter"
}

function Iniciar-Servicio {
    param([string]$Carpeta, [hashtable]$Extra = @{})
    $vars = "`$env:JWT_SECRET='$jwtSecret';"
    foreach ($k in $Extra.Keys) { $vars += " `$env:$k='$($Extra[$k])';" }
    $cmd = "$vars Set-Location '$raiz\$Carpeta'; `$Host.UI.RawUI.WindowTitle='$Carpeta'; mvn spring-boot:run"
    Start-Process powershell -ArgumentList '-NoExit', '-Command', $cmd
    Write-Host "Iniciando $Carpeta..."
}

function Esperar-Puerto {
    param([int]$Puerto, [string]$Nombre)
    Write-Host "Esperando a $Nombre (puerto $Puerto) " -NoNewline
    do {
        Start-Sleep -Seconds 3
        $ok = Test-NetConnection localhost -Port $Puerto -InformationLevel Quiet -WarningAction SilentlyContinue
        Write-Host "." -NoNewline
    } until ($ok)
    Write-Host " listo"
}

# 1) Registro de servicios
Iniciar-Servicio 'eureka-server'
Esperar-Puerto 8761 'Eureka'

# 2) Configuracion centralizada
Iniciar-Servicio 'config-server'
Esperar-Puerto 8888 'Config Server'

# 3) Gateway y servicios de negocio
Iniciar-Servicio 'api-gateway'
Iniciar-Servicio 'servicio-auth'     @{ SPRING_CLOUD_VAULT_ENABLED = 'false' }
Iniciar-Servicio 'servicio-clientes' @{ RENIEC_TOKEN = $reniecToken }
Iniciar-Servicio 'servicio-paquetes'

Esperar-Puerto 8080 'API Gateway'
Esperar-Puerto 8081 'servicio-auth'
Esperar-Puerto 8082 'servicio-clientes'
Esperar-Puerto 8083 'servicio-paquetes'

Write-Host ""
Write-Host "=============================================="
Write-Host " Todo listo."
Write-Host " Eureka:   http://localhost:8761"
Write-Host " Gateway:  http://localhost:8080"
Write-Host " Web:      https://rapidocourier.vercel.app"
Write-Host "           (o cd frontend; npm run dev)"
Write-Host "=============================================="
