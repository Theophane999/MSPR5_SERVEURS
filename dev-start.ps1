$ErrorActionPreference = 'Stop'

$root = Split-Path -Parent $MyInvocation.MyCommand.Path
$terraformDir = Join-Path $root 'terraform'
$frontendDir = Join-Path $root 'services\frontend'
$frontendPort = 4300

function Write-Step {
    param([string]$Message)
    Write-Host "`n==> $Message" -ForegroundColor Cyan
}

function Ensure-Command {
    param([string]$Name)
    if (-not (Get-Command $Name -ErrorAction SilentlyContinue)) {
        throw "Commande introuvable: $Name"
    }
}

function Ensure-DockerRunning {
    Ensure-Command 'docker'

    $dockerUp = $false
    try {
        docker info | Out-Null
        $dockerUp = $true
    } catch {
        $dockerUp = $false
    }

    if ($dockerUp) {
        Write-Host 'Docker daemon: OK' -ForegroundColor Green
        return
    }

    Write-Host 'Docker daemon non disponible. Tentative de lancement Docker Desktop...' -ForegroundColor Yellow
    $desktop = 'C:\Program Files\Docker\Docker\Docker Desktop.exe'
    if (-not (Test-Path $desktop)) {
        throw 'Docker Desktop introuvable. Demarre Docker manuellement puis relance ce script.'
    }

    Start-Process -FilePath $desktop | Out-Null

    $maxWaitSeconds = 120
    $elapsed = 0
    while ($elapsed -lt $maxWaitSeconds) {
        Start-Sleep -Seconds 3
        $elapsed += 3
        try {
            docker info | Out-Null
            Write-Host 'Docker daemon: OK' -ForegroundColor Green
            return
        } catch {
        }
    }

    throw 'Docker daemon toujours indisponible apres attente. Verifie Docker Desktop.'
}

function Ensure-TerraformInfra {
    Ensure-Command 'terraform'

    Write-Step 'Initialisation Terraform'
    Push-Location $terraformDir
    try {
        terraform init | Out-Host

        Write-Step 'Demarrage infra dev (BDD + backends + mqtt)'
        $applyArgs = @(
            'apply',
            '-auto-approve',
            '-target=docker_network.futurekawa',
            '-target=docker_volume.child_db_data',
            '-target=docker_container.mosquitto',
            '-target=docker_image.backend_child',
            '-target=docker_container.child_db',
            '-target=docker_container.backend_child',
            '-target=docker_image.backend_mother',
            '-target=docker_container.backend_mother'
        )
        terraform @applyArgs | Out-Host
    } finally {
        Pop-Location
    }
}

function Assert-ContainersRunning {
    $required = @(
        'postgres-brazil',
        'postgres-ecuador',
        'postgres-colombia',
        'backend-brazil',
        'backend-ecuador',
        'backend-colombia',
        'backend-mother',
        'mosquitto-broker'
    )

    Write-Step 'Verification des conteneurs critiques'
    foreach ($name in $required) {
        $running = ''
        try {
            $running = docker inspect -f "{{.State.Running}}" $name 2>$null
        } catch {
            $running = ''
        }

        if ($running -ne 'true') {
            throw "Conteneur non operationnel: $name"
        }
        Write-Host "OK: $name" -ForegroundColor Green
    }
}

function Check-ApiHealth {
    Write-Step 'Verification API mere/fille'

    $mother = Invoke-RestMethod -Uri 'http://localhost:3200/health' -TimeoutSec 10
    $child = Invoke-RestMethod -Uri 'http://localhost:3101/health' -TimeoutSec 10

    if ($mother.status -ne 'ok') { throw 'Backend mere non sain' }
    if ($child.status -ne 'ok') { throw 'Backend fille non sain' }

    Write-Host "Mother health: $($mother | ConvertTo-Json -Compress)" -ForegroundColor Green
    Write-Host "Child health:  $($child | ConvertTo-Json -Compress)" -ForegroundColor Green
}

function Ensure-DemoSensorData {
    Write-Step 'Verification des donnees capteurs de demo'

    $brazilCount = (Invoke-RestMethod -Uri 'http://localhost:3101/api/capteurs' -TimeoutSec 10).Count
    $ecuadorCount = (Invoke-RestMethod -Uri 'http://localhost:3102/api/capteurs' -TimeoutSec 10).Count
    $colombiaCount = (Invoke-RestMethod -Uri 'http://localhost:3103/api/capteurs' -TimeoutSec 10).Count

    if (($brazilCount + $ecuadorCount + $colombiaCount) -gt 0) {
        Write-Host "Donnees capteurs detectees (B=$brazilCount E=$ecuadorCount C=$colombiaCount)" -ForegroundColor Green
        return
    }

    Write-Host 'Aucune donnee capteur detectee, injection d un jeu de demo...' -ForegroundColor Yellow

    $targets = @(
        @{ container = 'postgres-brazil'; db = 'futurekawa_brazil'; entrepot = 1; baseT = 24.2; baseH = 58.0 },
        @{ container = 'postgres-ecuador'; db = 'futurekawa_ecuador'; entrepot = 2; baseT = 26.4; baseH = 64.0 },
        @{ container = 'postgres-colombia'; db = 'futurekawa_colombia'; entrepot = 3; baseT = 22.8; baseH = 55.0 }
    )

    foreach ($t in $targets) {
        $sql = @"
INSERT INTO \"capteur\" (\"humidité\", \"temperature\", \"date\", \"ID_entrepot\") VALUES
($($t.baseH-3), $($t.baseT-1.4), CURRENT_DATE - 5, $($t.entrepot)),
($($t.baseH-2), $($t.baseT-0.8), CURRENT_DATE - 4, $($t.entrepot)),
($($t.baseH-1), $($t.baseT-0.2), CURRENT_DATE - 3, $($t.entrepot)),
($($t.baseH),   $($t.baseT),     CURRENT_DATE - 2, $($t.entrepot)),
($($t.baseH+1), $($t.baseT+0.7), CURRENT_DATE - 1, $($t.entrepot)),
($($t.baseH+2), $($t.baseT+1.1), CURRENT_DATE,     $($t.entrepot));
"@
        docker exec -i $($t.container) psql -U futurekawa -d $($t.db) -c $sql | Out-Host
    }

    Write-Host 'Jeu de demo capteurs injecte.' -ForegroundColor Green
}

function Start-FrontendDev {
    Ensure-Command 'npm.cmd'

    Write-Step "Demarrage frontend visuel en mode dev (port $frontendPort)"

    $frontendCmd = @(
        "Set-Location '$frontendDir'",
        "if (-not (Test-Path 'node_modules')) { npm.cmd install }",
        "npm.cmd run start -- --port $frontendPort"
    ) -join '; '

    Start-Process powershell -ArgumentList '-NoExit', '-Command', $frontendCmd | Out-Null

    Start-Sleep -Seconds 2
    Start-Process "http://localhost:$frontendPort" | Out-Null

    Write-Host "Frontend dev lance sur http://localhost:$frontendPort" -ForegroundColor Green
}

Write-Step 'Pre-check Docker'
Ensure-DockerRunning

Ensure-TerraformInfra
Assert-ContainersRunning
Check-ApiHealth
Ensure-DemoSensorData
Start-FrontendDev

Write-Host ''
Write-Host 'Environnement dev pret.' -ForegroundColor Green
Write-Host 'Backend mere:  http://localhost:3200/api/children'
Write-Host 'Backend filles: http://localhost:3101, http://localhost:3102, http://localhost:3103'
Write-Host 'Frontend dev:  http://localhost:4300'
