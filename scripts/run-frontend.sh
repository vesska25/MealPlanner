#!/usr/bin/env bash
# Runs the React/TypeScript web client (PRD step 11 Phase B) against the local backend.
# Expects the backend to already be running (see run-meal-planning.sh's sibling scripts, or
# `cd MealPlanner && ./mvnw spring-boot:run` directly) on http://localhost:8080.
#
# Usage: scripts/run-frontend.sh
set -euo pipefail
cd "$(dirname "$0")/.."

cd frontend

if [ ! -d node_modules ]; then
  npm install
fi

npm run dev
