#!/usr/bin/env bash
# Runs the real meal-planning agent (PRD 9.1 step 7) against the live Anthropic API.
# Loads config from .env (gitignored) so the API key never has to be pasted into a shell
# command or committed anywhere. See .env.example for the expected keys.
#
# Usage: scripts/run-meal-planning.sh ["optional question for the model"]
set -euo pipefail
cd "$(dirname "$0")/.."

if [ -f .env ]; then
  set -a
  # shellcheck disable=SC1091
  source .env
  set +a
fi

if [ -z "${ANTHROPIC_API_KEY:-}" ]; then
  echo "ANTHROPIC_API_KEY is not set. Add it to .env (copy .env.example if you don't have one)." >&2
  exit 1
fi

export SPRING_PROFILES_ACTIVE=meal-planning

mvn_args=()
if [ -n "${AGENT_JVM_ARGS:-}" ]; then
  mvn_args+=("-Dspring-boot.run.jvmArguments=$AGENT_JVM_ARGS")
fi
if [ "$#" -gt 0 ]; then
  mvn_args+=("-Dspring-boot.run.arguments=$*")
fi

cd MealPlanner
./mvnw spring-boot:run "${mvn_args[@]}"
