#!/usr/bin/env bash
set -euo pipefail

# docs:start
wait_for_job() {
  while true; do
    STATUS=$(curl -s -S "https://api.docudevs.ai/job/status/$1" \
      -H "Authorization: $API_KEY" | jq -r '.status')
    if [ "$STATUS" = "COMPLETED" ]; then return 0; fi
    if [ "$STATUS" = "ERROR" ]; then echo "Job $1 failed" >&2; return 1; fi
    sleep 5
  done
}

JOB_GUID=$(curl -s -S -X POST "https://api.docudevs.ai/document/upload" \
  -H "Authorization: $API_KEY" \
  -F "document=@large-report.pdf" | jq -r '.guid')

# Tracing covers every chunk
curl -s -S -f -o /dev/null -X POST "https://api.docudevs.ai/document/process/$JOB_GUID" \
  -H "Authorization: $API_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "prompt": "Extract line items",
    "mimeType": "application/pdf",
    "trace": true,
    "mapReduce": {
      "enabled": true,
      "splitType": "PAGE",
      "pagesPerChunk": 5
    }
  }'

wait_for_job "$JOB_GUID"

curl -s -S -f "https://api.docudevs.ai/job/trace/$JOB_GUID" \
  -H "Authorization: $API_KEY" | jq -r '"Processed in \(.events | length) events"'
# docs:end
