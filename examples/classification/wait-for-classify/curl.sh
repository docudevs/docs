#!/usr/bin/env bash
set -euo pipefail

# docs:start
CASE_ID=42  # id of a records case
JOB_ID=9  # jobId returned when the job was started

DEADLINE=$((SECONDS + 3600))  # same limit as the Python timeout
while [ "$SECONDS" -lt "$DEADLINE" ]; do
  JOB=$(curl -s -S -f "https://api.docudevs.ai/cases/$CASE_ID/classify/$JOB_ID" \
    -H "Authorization: $API_KEY")
  STATUS=$(echo "$JOB" | jq -r '.status')
  case "$STATUS" in
    COMPLETED) echo "$JOB" | jq '{status, counts, usage}'; exit 0 ;;
    FAILED|CANCELLED) echo "Classify job $JOB_ID ended with status $STATUS" >&2; exit 1 ;;
  esac
  sleep 2
done
echo "Classify job $JOB_ID did not complete in time" >&2
exit 1
# docs:end
