#!/usr/bin/env bash
set -euo pipefail

# docs:start
wait_for_job() {
  while true; do
    STATUS=$(curl -s -S -f "https://api.docudevs.ai/job/status/$1" \
      -H "Authorization: $API_KEY" | jq -r '.status')
    case "$STATUS" in
      COMPLETED) return 0 ;;
      ERROR|FAILED|TIMEOUT|PARTIAL) echo "Job $1 ended with status $STATUS" >&2; return 1 ;;
    esac
    sleep 5
  done
}

JOB_GUID=$(curl -s -S -f -X POST "https://api.docudevs.ai/document/upload" \
  -H "Authorization: $API_KEY" \
  -F "document=@report.pdf" | jq -r '.guid')

# The document has 20 pages, but only pages 1-2, 5-6 and 10-11 are needed.
# These 6 pages are chunked into 3 chunks of 2 pages each.
curl -s -S -f -o /dev/null -X POST "https://api.docudevs.ai/document/process/$JOB_GUID" \
  -H "Authorization: $API_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "prompt": "Extract all items from these pages.",
    "mimeType": "application/pdf",
    "pageRange": [1, 2, 5, 6, 10, 11],
    "mapReduce": {
      "enabled": true,
      "splitType": "PAGE",
      "pagesPerChunk": 2
    }
  }'

wait_for_job "$JOB_GUID"

curl -s -S -f "https://api.docudevs.ai/job/result/$JOB_GUID/json" \
  -H "Authorization: $API_KEY" | jq .
# docs:end
