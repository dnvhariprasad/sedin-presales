#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
RUN_DIR="$ROOT_DIR/.run"
PID_FILE="$RUN_DIR/dev-servers.pid"
BACKEND_LOG="$RUN_DIR/backend.log"
FRONTEND_LOG="$RUN_DIR/frontend.log"

BACKEND_PORT="${BACKEND_PORT:-8080}"
FRONTEND_PORT="${FRONTEND_PORT:-3010}"
FORCE_KILL=0

usage() {
  cat <<EOF
Usage: $(basename "$0") [--force]

Starts backend (Spring Boot, port ${BACKEND_PORT}) and frontend (Vite, port ${FRONTEND_PORT}) together.

Options:
  --force   Kill processes already listening on required ports before startup
EOF
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --force)
      FORCE_KILL=1
      shift
      ;;
    -h|--help)
      usage
      exit 0
      ;;
    *)
      echo "Unknown option: $1" >&2
      usage
      exit 1
      ;;
  esac
done

require_cmd() {
  if ! command -v "$1" >/dev/null 2>&1; then
    echo "Missing required command: $1" >&2
    exit 1
  fi
}

port_pids() {
  local port="$1"
  lsof -tiTCP:"$port" -sTCP:LISTEN 2>/dev/null || true
}

ensure_port_free() {
  local port="$1"
  local name="$2"
  local pids
  pids="$(port_pids "$port")"

  if [[ -n "$pids" ]]; then
    if [[ "$FORCE_KILL" -eq 1 ]]; then
      echo "Port ${port} (${name}) is in use. Killing process(es): ${pids}"
      # shellcheck disable=SC2086
      kill $pids 2>/dev/null || true
      sleep 1
    else
      echo "Port ${port} (${name}) is already in use by: ${pids}" >&2
      echo "Use --force to stop existing process(es)." >&2
      exit 1
    fi
  fi

  if [[ -n "$(port_pids "$port")" ]]; then
    echo "Unable to free port ${port} (${name})." >&2
    exit 1
  fi
}

wait_for_port() {
  local port="$1"
  local label="$2"
  local timeout="${3:-60}"

  local elapsed=0
  while (( elapsed < timeout )); do
    if [[ -n "$(port_pids "$port")" ]]; then
      return 0
    fi
    sleep 1
    ((elapsed+=1))
  done

  echo "${label} did not start on port ${port} within ${timeout}s." >&2
  return 1
}

cleanup() {
  local exit_code=$?

  if [[ -f "$PID_FILE" ]]; then
    # shellcheck disable=SC1090
    source "$PID_FILE"

    if [[ -n "${BACKEND_PID:-}" ]] && kill -0 "${BACKEND_PID}" 2>/dev/null; then
      kill "${BACKEND_PID}" 2>/dev/null || true
    fi

    if [[ -n "${FRONTEND_PID:-}" ]] && kill -0 "${FRONTEND_PID}" 2>/dev/null; then
      kill "${FRONTEND_PID}" 2>/dev/null || true
    fi

    rm -f "$PID_FILE"
  fi

  exit "$exit_code"
}

require_cmd lsof
require_cmd mvn
require_cmd npm

mkdir -p "$RUN_DIR"

if [[ -f "$PID_FILE" ]]; then
  echo "Found existing PID file: $PID_FILE" >&2
  echo "Run scripts/stop-dev.sh first (or remove stale pid file)." >&2
  exit 1
fi

ensure_port_free "$BACKEND_PORT" "backend"
ensure_port_free "$FRONTEND_PORT" "frontend"

: > "$BACKEND_LOG"
: > "$FRONTEND_LOG"

echo "Starting backend on port ${BACKEND_PORT}..."
(
  cd "$ROOT_DIR"
  mvn -B spring-boot:run -Dspring-boot.run.profiles=dev -f src/backend/pom.xml
) >>"$BACKEND_LOG" 2>&1 &
BACKEND_PID=$!

echo "Starting frontend on port ${FRONTEND_PORT}..."
(
  cd "$ROOT_DIR/src/frontend"
  npm run dev -- --host 0.0.0.0 --port "$FRONTEND_PORT" --strictPort
) >>"$FRONTEND_LOG" 2>&1 &
FRONTEND_PID=$!

cat >"$PID_FILE" <<EOF
BACKEND_PID=${BACKEND_PID}
FRONTEND_PID=${FRONTEND_PID}
BACKEND_PORT=${BACKEND_PORT}
FRONTEND_PORT=${FRONTEND_PORT}
EOF

trap cleanup INT TERM EXIT

wait_for_port "$BACKEND_PORT" "Backend" 90
wait_for_port "$FRONTEND_PORT" "Frontend" 90

echo
echo "Backend started:  http://localhost:${BACKEND_PORT}"
echo "Frontend started: http://localhost:${FRONTEND_PORT}"
echo "Backend log:      ${BACKEND_LOG}"
echo "Frontend log:     ${FRONTEND_LOG}"
echo
echo "Press Ctrl+C to stop both services."

while true; do
  if ! kill -0 "$BACKEND_PID" 2>/dev/null; then
    echo "Backend process exited. Stopping frontend..." >&2
    exit 1
  fi
  if ! kill -0 "$FRONTEND_PID" 2>/dev/null; then
    echo "Frontend process exited. Stopping backend..." >&2
    exit 1
  fi
  sleep 2
done

