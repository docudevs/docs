#!/usr/bin/env bash
set -euo pipefail

# docs:start
CASE_ID=42  # id of a records case with an active category tree

wait_for_classify() {
  local deadline=$((SECONDS + 3600))  # same limit as the Python timeout
  while [ "$SECONDS" -lt "$deadline" ]; do
    STATUS=$(curl -s -S -f "https://api.docudevs.ai/cases/$CASE_ID/classify/$1" \
      -H "Authorization: $API_KEY" | jq -r '.status')
    case "$STATUS" in
      COMPLETED) return 0 ;;
      FAILED|CANCELLED) echo "Classify job $1 ended with status $STATUS" >&2; return 1 ;;
    esac
    sleep 5
  done
  echo "Classify job $1 did not complete in time" >&2
  return 1
}

JOB_ID=$(curl -s -S -f -X POST "https://api.docudevs.ai/cases/$CASE_ID/classify" \
  -H "Authorization: $API_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "keys": ["SKU-1001", "SKU-1002", "SKU-1003"],
    "config": {
      "maxTotalTokens": 400000,
      "investigate": {"enabled": true, "maxRows": 10}
    }
  }' | jq -r '.jobId')

wait_for_classify "$JOB_ID"
echo "Classify job $JOB_ID completed; the investigation run starts from it"
# docs:end
