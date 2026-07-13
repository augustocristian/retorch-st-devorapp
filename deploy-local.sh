#!/usr/bin/env bash
# deploy-local.sh — Deploy or tear down the DevorApp SUT locally
#
# Usage:
#   ./deploy-local.sh                — start on default port 80 (frontend)
#   ./deploy-local.sh --port 8080    — custom frontend port
#   ./deploy-local.sh --down         — tear down the running deployment

set -euo pipefail

TJOB_NAME="default"
NETWORK_NAME="jenkins_network"
SUT_REPO="https://gitlab.com/HP-SCDS/Observatorio/2025-2026/devorapp/epi-devorapp.git"
SUT_DIR="devorapp"
ENV_FILE=".retorch/envfiles/local.env"
COMPOSE_FILE="docker-compose.yml"
MAX_WAIT_SECS=120
POLL_INTERVAL=5
PORT=80
DOWN=false
HEALTH_URL="http://localhost:8000/health"

step() { echo "[>] $*"; }
ok()   { echo "[+] $*"; }
fail() { echo "[!] $*" >&2; exit 1; }

# ── Parse arguments ─────────────────────────────────────────────────────────────
while [[ $# -gt 0 ]]; do
    case "$1" in
        --down)  DOWN=true; shift ;;
        --port)  PORT="$2"; shift 2 ;;
        *) fail "Unknown argument: $1. Valid options: --down, --port <number>" ;;
    esac
done

# ── Prerequisites ────────────────────────────────────────────────────────────────
step "Checking prerequisites..."
command -v docker >/dev/null 2>&1 || fail "docker is not installed."
command -v git    >/dev/null 2>&1 || fail "git is not installed."
docker info >/dev/null 2>&1       || fail "Docker daemon is not running."
ok "Prerequisites satisfied."

# ── Ensure env file exists ───────────────────────────────────────────────────────
if [[ ! -f "$ENV_FILE" ]]; then
    step "Creating empty env file at $ENV_FILE ..."
    mkdir -p "$(dirname "$ENV_FILE")"
    touch "$ENV_FILE"
    ok "Created $ENV_FILE (add FIREBASE_API_KEY, GOOGLE_API_KEY etc. as needed)."
fi

# ── Export env vars for docker compose ──────────────────────────────────────────
export TJOB_NAME
export frontend_port=$PORT

# ── Teardown mode ────────────────────────────────────────────────────────────────
if $DOWN; then
    step "Tearing down project '$TJOB_NAME'..."
    docker compose -f "$COMPOSE_FILE" --env-file "$ENV_FILE" --ansi never -p "$TJOB_NAME" down --volumes
    ok "Teardown complete."
    exit 0
fi

# ── Clone SUT if needed ──────────────────────────────────────────────────────────
step "Checking SUT directory '$SUT_DIR'..."
if [[ ! -d "$SUT_DIR" ]]; then
    step "Cloning $SUT_REPO into '$SUT_DIR'..."
    git clone "$SUT_REPO" "$SUT_DIR"
    ok "Cloned SUT."
else
    ok "'$SUT_DIR' already present, skipping clone."
fi

# ── Patch SUT if needed ──────────────────────────────────────────────────────────
step "Checking SUT patches..."
CONFIG="$SUT_DIR/backend/app/core/config.py"
AUTH="$SUT_DIR/backend/app/services/auth_service.py"
DOCKERFILE="$SUT_DIR/backend/Dockerfile"

if [[ -f "$CONFIG" ]] && ! grep -q "SKIP_EMAIL_VERIFICATION" "$CONFIG"; then
    step "Patching config.py..."
    sed -i 's/    # JWT/    # Test helpers\n    SKIP_EMAIL_VERIFICATION: bool = False\n\n    # JWT/' "$CONFIG"
    ok "Patched config.py"
fi

if [[ -f "$AUTH" ]] && ! grep -q "SKIP_EMAIL_VERIFICATION" "$AUTH"; then
    step "Patching auth_service.py..."
    sed -i 's/if not user_record\.email_verified:/if not settings.SKIP_EMAIL_VERIFICATION and not user_record.email_verified:/' "$AUTH"
    ok "Patched auth_service.py"
fi

if [[ -f "$DOCKERFILE" ]] && ! grep -q "firebase-service-account.json" "$DOCKERFILE"; then
    step "Patching Dockerfile..."
    sed -i 's/COPY entrypoint.sh .\/entrypoint.sh/COPY entrypoint.sh .\/entrypoint.sh\nCOPY firebase-service-account.json\* .\//' "$DOCKERFILE"
    ok "Patched Dockerfile"
fi

# ── External Docker network ──────────────────────────────────────────────────────
step "Ensuring Docker network '$NETWORK_NAME' exists..."
if ! docker network ls --format "{{.Name}}" | grep -qx "$NETWORK_NAME"; then
    docker network create "$NETWORK_NAME"
    ok "Network '$NETWORK_NAME' created."
else
    ok "Network '$NETWORK_NAME' already exists."
fi

# ── Build images ─────────────────────────────────────────────────────────────────
if [[ -f "firebase-service-account.json" ]]; then
    step "Copying firebase-service-account.json from root to SUT backend folder..."
    cp "firebase-service-account.json" "$SUT_DIR/backend/"
fi

step "Building Docker images..."
docker compose -f "$COMPOSE_FILE" --env-file "$ENV_FILE" --ansi never -p "$TJOB_NAME" build
ok "Images built."

# ── Start containers ─────────────────────────────────────────────────────────────
step "Starting containers (project: '$TJOB_NAME', frontend port: $PORT)..."
docker compose -f "$COMPOSE_FILE" --env-file "$ENV_FILE" --ansi never -p "$TJOB_NAME" up -d
ok "Containers started."

# ── Wait for SUT ─────────────────────────────────────────────────────────────────
elapsed=0
ready=false

step "Waiting for DevorApp backend at $HEALTH_URL (up to ${MAX_WAIT_SECS}s)..."
while [[ $elapsed -lt $MAX_WAIT_SECS ]]; do
    if curl --silent --max-time 5 "$HEALTH_URL" 2>/dev/null | grep -q '"status".*"ok"'; then
        ready=true
        break
    fi
    sleep $POLL_INTERVAL
    elapsed=$((elapsed + POLL_INTERVAL))
    echo "  ... $elapsed / ${MAX_WAIT_SECS}s"
done

if $ready; then
    ok "DevorApp backend is ready at $HEALTH_URL"
else
    echo "[!] SUT did not become healthy within ${MAX_WAIT_SECS}s." >&2
    step "Collecting container logs (last 50 lines)..."
    docker compose -f "$COMPOSE_FILE" --env-file "$ENV_FILE" --ansi never -p "$TJOB_NAME" logs --tail 50
    step "Tearing down failed deployment..."
    docker compose -f "$COMPOSE_FILE" --env-file "$ENV_FILE" --ansi never -p "$TJOB_NAME" down --volumes
    exit 1
fi
