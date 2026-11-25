#!/usr/bin/env bash
set -euo pipefail

# docs:start
wait_for_activation() {
  local deadline=$((SECONDS + 3600))  # same limit as the Python timeout
  while [ "$SECONDS" -lt "$deadline" ]; do
    STATUS=$(curl -s -S -f "https://api.docudevs.ai/job/status/$1" \
      -H "Authorization: $API_KEY" | jq -r '.status')
    case "$STATUS" in
      COMPLETED) echo "Activation job $1: $STATUS"; return 0 ;;
      ERROR|FAILED|TIMEOUT) echo "Activation job $1 ended with status $STATUS" >&2; return 1 ;;
    esac
    sleep 2
  done
  echo "Activation job $1 did not complete in time" >&2
  return 1
}

CASE_ID=42  # id of a records case
VERSION_ID=7  # treeVersionId returned when the tree was uploaded

JOB_GUID=$(curl -s -S -f -X POST "https://api.docudevs.ai/cases/$CASE_ID/trees/$VERSION_ID/activate" \
  -H "Authorization: $API_KEY" \
  -H "Content-Type: application/json" \
  -d '{"successorMapping": {"9.90.1": "4.12.3"}, "allowMissing": false}' | jq -r '.jobGuid')

wait_for_activation "$JOB_GUID"
# docs:end
