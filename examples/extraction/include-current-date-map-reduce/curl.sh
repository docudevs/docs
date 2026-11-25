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

SCHEMA='{
  "type": "object",
  "properties": {
    "line_items": {
      "type": "array",
      "items": {
        "type": "object",
        "properties": {
          "description": {"type": "string"},
          "warranty_end_date": {"type": "string", "format": "date"},
          "remaining_warranty_days": {"type": "number"}
        }
      }
    }
  }
}'

JOB_GUID=$(curl -s -S -f -X POST "https://api.docudevs.ai/document/upload" \
  -H "Authorization: $API_KEY" \
  -F "document=@warranty-list.pdf" | jq -r '.guid')

# Every chunk receives the current date
jq -n --arg schema "$SCHEMA" \
  '{
    mimeType: "application/pdf",
    prompt: "Extract line items with remaining warranty days",
    schema: $schema,
    includeCurrentDate: true,
    mapReduce: {enabled: true, splitType: "PAGE", pagesPerChunk: 2}
  }' |
  curl -s -S -f -o /dev/null -X POST "https://api.docudevs.ai/document/process/$JOB_GUID" \
    -H "Authorization: $API_KEY" \
    -H "Content-Type: application/json" \
    --data-binary @-

wait_for_job "$JOB_GUID"

curl -s -S -f "https://api.docudevs.ai/job/result/$JOB_GUID/json" \
  -H "Authorization: $API_KEY" | jq .
# docs:end
