#!/usr/bin/env bash
# Trust Agro ERP — local dev startup (H2 database, no PostgreSQL required)
set -euo pipefail

ROOT="$(cd "$(dirname "$0")" && pwd)"
BACKEND_PORT="${BACKEND_PORT:-8081}"
FRONTEND_PORT="${FRONTEND_PORT:-3000}"

echo "==> Starting backend (Spring profile: local, port: $BACKEND_PORT)..."
cd "$ROOT/backend"
SERVER_PORT="$BACKEND_PORT" mvn spring-boot:run -Dspring-boot.run.profiles=local -q &
BACKEND_PID=$!

echo "==> Waiting for backend on http://localhost:$BACKEND_PORT ..."
for i in $(seq 1 60); do
  if curl -sf "http://localhost:$BACKEND_PORT/actuator/health" >/dev/null 2>&1 \
     || curl -sf -o /dev/null -w '' "http://localhost:$BACKEND_PORT/api/auth/login" 2>/dev/null; then
    echo "    Backend ready."
    break
  fi
  if ! kill -0 "$BACKEND_PID" 2>/dev/null; then
    echo "    Backend process exited. Check logs above."
    exit 1
  fi
  sleep 2
done

echo "==> Starting frontend (port: $FRONTEND_PORT)..."
cd "$ROOT/frontend"
REACT_APP_API_URL="http://localhost:$BACKEND_PORT" PORT="$FRONTEND_PORT" npm start &
FRONTEND_PID=$!

trap 'kill $BACKEND_PID $FRONTEND_PID 2>/dev/null' EXIT INT TERM
wait
