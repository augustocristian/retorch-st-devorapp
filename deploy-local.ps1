<#
.SYNOPSIS
    Deploys or tears down the DevorApp SUT locally using Docker Compose.
.PARAMETER Down
    Tear down the running deployment instead of starting it.
.PARAMETER Port
    Host port to expose the nginx frontend on (default: 80).
.EXAMPLE
    .\deploy-local.ps1
    .\deploy-local.ps1 -Port 8080
    .\deploy-local.ps1 -Down
#>
param(
    [switch]$Down,
    [int]$Port = 80
)

$ErrorActionPreference = "Stop"

$TJOB_NAME    = "default"
$NETWORK_NAME = "jenkins_network"
$SUT_REPO     = "https://gitlab.com/HP-SCDS/Observatorio/2025-2026/devorapp/epi-devorapp.git"
$SUT_DIR      = "devorapp"
$ENV_FILE     = ".retorch/envfiles/local.env"
$COMPOSE_FILE = "docker-compose.yml"
$MAX_WAIT_SECS = 120
$POLL_INTERVAL = 5
$HEALTH_URL   = "http://localhost:8000/health"

function Write-Step([string]$msg) { Write-Host "[>] $msg" -ForegroundColor Cyan }
function Write-OK([string]$msg)   { Write-Host "[+] $msg" -ForegroundColor Green }
function Write-Fail([string]$msg) { Write-Host "[!] $msg" -ForegroundColor Red; exit 1 }

# ── Prerequisites ───────────────────────────────────────────────────────────────
Write-Step "Checking prerequisites..."
if (-not (Get-Command docker -ErrorAction SilentlyContinue)) { Write-Fail "docker is not installed or not in PATH." }
$ErrorActionPreference = "Continue"
docker info *>$null
$dockerOk = ($LASTEXITCODE -eq 0)
$ErrorActionPreference = "Stop"
if (-not $dockerOk) { Write-Fail "Docker daemon is not running. Please start Docker Desktop." }
if (-not (Get-Command git -ErrorAction SilentlyContinue)) { Write-Fail "git is not installed or not in PATH." }
Write-OK "Prerequisites satisfied."

# ── Ensure env file exists ──────────────────────────────────────────────────────
if (-not (Test-Path $ENV_FILE)) {
    Write-Step "Creating empty env file at $ENV_FILE ..."
    New-Item -ItemType Directory -Force -Path (Split-Path $ENV_FILE) | Out-Null
    New-Item -ItemType File -Path $ENV_FILE | Out-Null
    Write-OK "Created $ENV_FILE (add FIREBASE_API_KEY, GOOGLE_API_KEY etc. as needed)."
}

# ── Export env vars for docker compose ─────────────────────────────────────────
$env:TJOB_NAME     = $TJOB_NAME
$env:frontend_port = $Port

# ── Teardown mode ───────────────────────────────────────────────────────────────
if ($Down) {
    Write-Step "Tearing down project '$TJOB_NAME'..."
    docker compose -f $COMPOSE_FILE --env-file $ENV_FILE --ansi never -p $TJOB_NAME down --volumes
    if ($LASTEXITCODE -ne 0) { Write-Fail "docker compose down failed." }
    Write-OK "Teardown complete."
    exit 0
}

# ── Clone SUT if needed ─────────────────────────────────────────────────────────
Write-Step "Checking SUT directory '$SUT_DIR'..."
if (-not (Test-Path $SUT_DIR)) {
    Write-Step "Cloning $SUT_REPO into '$SUT_DIR'..."
    git clone $SUT_REPO $SUT_DIR
    if ($LASTEXITCODE -ne 0) { Write-Fail "Failed to clone '$SUT_REPO'." }
    Write-OK "Cloned SUT."

    Write-Step "Patching SUT for test environment (SKIP_EMAIL_VERIFICATION)..."
    $configPath = "$SUT_DIR/backend/app/core/config.py"
    $authPath   = "$SUT_DIR/backend/app/services/auth_service.py"

    # Add SKIP_EMAIL_VERIFICATION field to Settings
    (Get-Content $configPath) -replace '(    # JWT)', "    # Test helpers`n    SKIP_EMAIL_VERIFICATION: bool = False`n`n`$1" |
        Set-Content $configPath
    Write-OK "Patched config.py"

    # Guard the email-verified check with the new setting
    (Get-Content $authPath) -replace 'if not user_record\.email_verified:',
        'if not settings.SKIP_EMAIL_VERIFICATION and not user_record.email_verified:' |
        Set-Content $authPath
    Write-OK "Patched auth_service.py"
} else {
    Write-OK "'$SUT_DIR' already present, skipping clone."
}

# ── External Docker network ─────────────────────────────────────────────────────
Write-Step "Ensuring Docker network '$NETWORK_NAME' exists..."
$existingNetworks = docker network ls --format "{{.Name}}"
if ($existingNetworks -notcontains $NETWORK_NAME) {
    docker network create $NETWORK_NAME
    if ($LASTEXITCODE -ne 0) { Write-Fail "Failed to create Docker network '$NETWORK_NAME'." }
    Write-OK "Network '$NETWORK_NAME' created."
} else {
    Write-OK "Network '$NETWORK_NAME' already exists."
}

# ── Build images ────────────────────────────────────────────────────────────────
Write-Step "Building Docker images..."
docker compose -f $COMPOSE_FILE --env-file $ENV_FILE --ansi never build
if ($LASTEXITCODE -ne 0) { Write-Fail "docker compose build failed." }
Write-OK "Images built."

# ── Start containers ────────────────────────────────────────────────────────────
Write-Step "Starting containers (project: '$TJOB_NAME', frontend port: $Port)..."
docker compose -f $COMPOSE_FILE --env-file $ENV_FILE --ansi never -p $TJOB_NAME up -d
if ($LASTEXITCODE -ne 0) { Write-Fail "docker compose up failed." }
Write-OK "Containers started."

# ── Wait for SUT ────────────────────────────────────────────────────────────────
$elapsed = 0
$ready   = $false

Write-Step "Waiting for DevorApp backend at $HEALTH_URL (up to ${MAX_WAIT_SECS}s)..."
while ($elapsed -lt $MAX_WAIT_SECS) {
    try {
        $resp = Invoke-WebRequest -Uri $HEALTH_URL -UseBasicParsing -TimeoutSec 5 -ErrorAction Stop
        if ($resp.Content -match '"status"\s*:\s*"ok"') { $ready = $true; break }
    } catch {}
    Start-Sleep -Seconds $POLL_INTERVAL
    $elapsed += $POLL_INTERVAL
    Write-Host "  ... $elapsed / ${MAX_WAIT_SECS}s"
}

if ($ready) {
    Write-OK "DevorApp backend is ready at $HEALTH_URL"
} else {
    Write-Host "[!] SUT did not become healthy within ${MAX_WAIT_SECS}s." -ForegroundColor Red
    Write-Step "Collecting container logs (last 50 lines)..."
    docker compose -f $COMPOSE_FILE --env-file $ENV_FILE --ansi never -p $TJOB_NAME logs --tail 50
    Write-Step "Tearing down failed deployment..."
    docker compose -f $COMPOSE_FILE --env-file $ENV_FILE --ansi never -p $TJOB_NAME down --volumes
    exit 1
}
