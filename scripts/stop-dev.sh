#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
RUN_DIR="$ROOT_DIR/.run"
PID_FILE="$RUN_DIR/dev-servers.pid"

if [[ ! -f "$PID_FILE" ]]; then
  echo "No PID file found at ${PID_FILE}. Nothing to stop."
  exit 0
fi

# shellcheck disable=SC1090
source "$PID_FILE"

stop_pid() {
  local pid="$1"
  local name="$2"

  if [[ -z "${pid}" ]]; then
    return 0
  fi

  if kill -0 "$pid" 2>/dev/null; then
    echo "Stopping ${name} (PID ${pid})..."
    kill "$pid" 2>/dev/null || true
  else
    echo "${name} PID ${pid} is not running."
  fi
}

stop_pid "${BACKEND_PID:-}" "backend"
stop_pid "${FRONTEND_PID:-}" "frontend"

rm -f "$PID_FILE"
echo "Stopped dev services."

