#!/usr/bin/env bash
set -euo pipefail

# docs:start
wait_for_job() {
  local deadline=$((SECONDS + 600))
  while [ "$SECONDS" -lt "$deadline" ]; do
    STATUS=$(curl -s -S -f "https://api.docudevs.ai/job/status/$1" \
      -H "Authorization: $API_KEY" | jq -r '.status')
    case "$STATUS" in
      COMPLETED) return 0 ;;
      PARTIAL) echo "Job $1 ended with status PARTIAL" >&2; return 0 ;;
      ERROR|FAILED|TIMEOUT) echo "Job $1 ended with status $STATUS" >&2; return 1 ;;
    esac
    sleep 5
  done
  echo "Job $1 did not finish within 600 seconds" >&2
  return 1
}

EXISTING_JOB_GUID="<job-guid>"  # GUID of a completed job from a previous run

# dependsOn chains the new run to the completed job; the job reuses its own GUID
curl -s -S -f -o /dev/null -X POST "https://api.docudevs.ai/document/process/$EXISTING_JOB_GUID?dependsOn=$EXISTING_JOB_GUID" \
  -H "Authorization: $API_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "prompt": "Extract all line items (sku, description, qty, total)",
    "schema": "{\"type\": \"array\", \"items\": {\"type\": \"object\"}}",
    "mimeType": "application/pdf",
    "mapReduce": {
      "enabled": true,
      "splitType": "PAGE",
      "pagesPerChunk": 5,
      "overlapPages": 1,
      "dedupKey": "sku",
      "parallelProcessing": true
    }
  }'

wait_for_job "$EXISTING_JOB_GUID"

RESULT=$(curl -s -S -f "https://api.docudevs.ai/job/result/$EXISTING_JOB_GUID/json" \
  -H "Authorization: $API_KEY")
echo "Header: $(jq -c '.header' <<< "$RESULT")"
echo "Extracted $(jq '.records | length' <<< "$RESULT") line items."
# docs:end
