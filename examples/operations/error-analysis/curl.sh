#!/usr/bin/env bash
set -euo pipefail

# docs:start
wait_for_job() {
  while true; do
    STATUS=$(curl -s -S "https://api.docudevs.ai/job/status/$1" \
      -H "Authorization: $API_KEY" | jq -r '.status')
    if [ "$STATUS" = "COMPLETED" ]; then return 0; fi
    if [ "$STATUS" = "ERROR" ]; then echo "Job $1 failed" >&2; return 1; fi
    sleep 2
  done
}

JOB_GUID="<job-guid>"  # GUID of a completed document job

OPERATION_JOB_GUID=$(curl -s -S -f -X POST "https://api.docudevs.ai/operation" \
  -H "Authorization: $API_KEY" \
  -H "Content-Type: application/json" \
  -d "{\"jobGuid\": \"$JOB_GUID\", \"type\": \"error-analysis\"}" | jq -r '.jobGuid')

wait_for_job "$OPERATION_JOB_GUID"

curl -s -S -f "https://api.docudevs.ai/operation/$JOB_GUID/error-analysis" \
  -H "Authorization: $API_KEY" | jq -r '.result'
# docs:end
