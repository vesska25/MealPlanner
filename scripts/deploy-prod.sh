#!/usr/bin/env bash
# Deploys the backend (Docker, via docker-compose.prod.yml) and frontend (static build) to
# mealplanner.mimosa-dev.de (PRD acceptance criterion #29). Manual trigger by design — no CI/CD
# yet. Assumes the one-time server setup (DNS, nginx site, cert, /var/www/mealplanner ownership,
# and a real .env alongside docker-compose.prod.yml on the server) is already done.
#
# Usage: MEALPLANNER_DEPLOY_KEY=/path/to/key scripts/deploy-prod.sh
set -euo pipefail
cd "$(dirname "$0")/.."

: "${MEALPLANNER_DEPLOY_KEY:?Set MEALPLANNER_DEPLOY_KEY to the path of the mealplanner-deploy SSH private key}"
HOST="mealplanner-deploy@152.53.158.15"
REMOTE_APP_DIR="mealplanner-app"
SSH="ssh -i $MEALPLANNER_DEPLOY_KEY"
SCP="scp -i $MEALPLANNER_DEPLOY_KEY"

echo "==> Building frontend (relative /api/ URLs — nginx proxies same-origin in prod)"
(cd frontend && npm ci && VITE_API_BASE_URL= npm run build)

echo "==> Packaging backend source + compose file"
BACKEND_TAR=$(mktemp /tmp/mealplanner-backend-XXXXXX.tar.gz)
tar --exclude='MealPlanner/target' -czf "$BACKEND_TAR" MealPlanner docker-compose.prod.yml

echo "==> Packaging frontend build output"
FRONTEND_TAR=$(mktemp /tmp/mealplanner-frontend-XXXXXX.tar.gz)
(cd frontend/dist && tar -czf "$FRONTEND_TAR" .)

echo "==> Uploading to server"
$SSH "$HOST" "mkdir -p $REMOTE_APP_DIR"
$SCP "$BACKEND_TAR" "$HOST:$REMOTE_APP_DIR/backend.tar.gz"
$SCP "$FRONTEND_TAR" "$HOST:/tmp/mealplanner-frontend.tar.gz"
rm -f "$BACKEND_TAR" "$FRONTEND_TAR"

echo "==> Deploying backend (docker compose up -d --build)"
$SSH "$HOST" "cd $REMOTE_APP_DIR && tar xzf backend.tar.gz && rm backend.tar.gz && docker compose -f docker-compose.prod.yml up -d --build"

echo "==> Deploying frontend static files to /var/www/mealplanner"
$SSH "$HOST" "rm -rf /var/www/mealplanner/* && tar xzf /tmp/mealplanner-frontend.tar.gz -C /var/www/mealplanner && rm /tmp/mealplanner-frontend.tar.gz"

echo "==> Done. https://mealplanner.mimosa-dev.de/"
echo "    Check backend logs with: $SSH $HOST 'cd $REMOTE_APP_DIR && docker compose -f docker-compose.prod.yml logs -f mealplanner-backend'"
