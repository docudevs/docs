#!/usr/bin/env bash
set -euo pipefail

# docs:start
# EXECUTION_ID is the "executionId" returned when the run was started.
EXECUTION_ID="managed-exec-00000000-0000-0000-0000-000000000000"

deadline=$((SECONDS + 600))
while [ "$SECONDS" -lt "$deadline" ]; do
  RUN=$(curl -s -S -f "https://api.docudevs.ai/managed-execution/$EXECUTION_ID" \
    -H "Authorization: $API_KEY")
  STATUS=$(echo "$RUN" | jq -r '.status')
  case "$STATUS" in
    COMPLETED|FAILED|CANCELLED|TIMEOUT) break ;;
  esac
  sleep 5
done

echo "$RUN" | jq '{status, durationSeconds, errorMessage}'
# docs:end
