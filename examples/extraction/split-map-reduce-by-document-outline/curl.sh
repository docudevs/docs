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

JOB_ID=$(curl -s -S -f -X POST "https://api.docudevs.ai/document/upload" \
  -H "Authorization: $API_KEY" \
  -F "document=@contract.pdf" | jq -r '.guid')

curl -s -S -f -o /dev/null -X POST "https://api.docudevs.ai/document/process/$JOB_ID" \
  -H "Authorization: $API_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "mimeType": "application/pdf",
    "prompt": "Extract obligations by section.",
    "mapReduce": {
      "enabled": true,
      "splitType": "DOCUMENT_OUTLINE",
      "pagesPerChunk": 3,
      "parallelProcessing": true
    }
  }'

wait_for_job "$JOB_ID"

curl -s -S -f "https://api.docudevs.ai/job/result/$JOB_ID/json" \
  -H "Authorization: $API_KEY" | jq .

curl -s -S -f "https://api.docudevs.ai/job/result/$JOB_ID/document-outline" \
  -H "Authorization: $API_KEY" | jq .
# docs:end
