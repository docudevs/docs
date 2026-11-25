#!/usr/bin/env bash
set -euo pipefail

# docs:start
wait_for_job() {
  while true; do
    STATUS=$(curl -s -S -f "https://api.docudevs.ai/job/status/$1" \
      -H "Authorization: $API_KEY" | jq -r '.status')
    case "$STATUS" in
      COMPLETED) return 0 ;;
      PARTIAL) echo "Job $1 ended with status PARTIAL" >&2; return 0 ;;
      ERROR|FAILED|TIMEOUT) echo "Job $1 ended with status $STATUS" >&2; return 1 ;;
    esac
    sleep 5
  done
}

JOB_GUID="<job-guid>"  # GUID of a completed document job

OPERATION_JOB_GUID=$(curl -s -S -f -X POST "https://api.docudevs.ai/operation" \
  -H "Authorization: $API_KEY" \
  -H "Content-Type: application/json" \
  -d "{
    \"jobGuid\": \"$JOB_GUID\",
    \"type\": \"document-outline\",
    \"parameters\": {
      \"customParameters\": {
        \"pagesPerChunk\": 3,
        \"overlapPages\": 1,
        \"parallelProcessing\": true
      }
    }
  }" | jq -r '.jobGuid')

wait_for_job "$OPERATION_JOB_GUID"

# The job result, then the outline artifact
curl -s -S -f "https://api.docudevs.ai/job/result/$OPERATION_JOB_GUID" \
  -H "Authorization: $API_KEY" | jq .

curl -s -S -f "https://api.docudevs.ai/job/result/$OPERATION_JOB_GUID/document-outline" \
  -H "Authorization: $API_KEY" | jq .
# docs:end
