#!/usr/bin/env bash
# Runs the throwaway agent-layer spike (PRD 9.1 step 3) against the real Anthropic API.
# Loads config from .env (gitignored) so the API key never has to be pasted into a shell
# command or committed anywhere. See .env.example for the expected keys.
#
# Usage: scripts/run-agent-spike.sh ["optional question for the model"]
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

export SPRING_PROFILES_ACTIVE=agent-spike

mvn_args=()
if [ -n "${AGENT_SPIKE_JVM_ARGS:-}" ]; then
  mvn_args+=("-Dspring-boot.run.jvmArguments=$AGENT_SPIKE_JVM_ARGS")
fi
if [ "$#" -gt 0 ]; then
  mvn_args+=("-Dspring-boot.run.arguments=$*")
fi

cd MealPlanner
./mvnw spring-boot:run "${mvn_args[@]}"
