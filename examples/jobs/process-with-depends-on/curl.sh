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

HEADER_SCHEMA='{
  "type": "object",
  "properties": {
    "invoice_number": {"type": "string"},
    "vendor": {"type": "string"},
    "total": {"type": "number"}
  }
}'

LINE_ITEMS_SCHEMA='{
  "type": "object",
  "properties": {
    "line_items": {
      "type": "array",
      "items": {
        "type": "object",
        "properties": {
          "description": {"type": "string"},
          "amount": {"type": "number"}
        }
      }
    }
  }
}'

# First extraction: parse the document and extract the header fields
JOB_GUID=$(curl -s -S -f -X POST "https://api.docudevs.ai/document/upload" \
  -H "Authorization: $API_KEY" \
  -F "document=@invoice.pdf" | jq -r '.guid')

jq -n --arg schema "$HEADER_SCHEMA" \
  '{mimeType: "application/pdf", prompt: "Extract invoice header fields", schema: $schema}' |
  curl -s -S -f -o /dev/null -X POST "https://api.docudevs.ai/document/process/$JOB_GUID" \
    -H "Authorization: $API_KEY" \
    -H "Content-Type: application/json" \
    --data-binary @-

wait_for_job "$JOB_GUID"
curl -s -S -f "https://api.docudevs.ai/job/result/$JOB_GUID/json" \
  -H "Authorization: $API_KEY" | jq .

# Second extraction: reuse the parsed document with another schema (no re-OCR)
jq -n --arg schema "$LINE_ITEMS_SCHEMA" \
  '{mimeType: "application/pdf", prompt: "Extract line items", schema: $schema}' |
  curl -s -S -f -o /dev/null -X POST "https://api.docudevs.ai/document/process/$JOB_GUID?dependsOn=$JOB_GUID" \
    -H "Authorization: $API_KEY" \
    -H "Content-Type: application/json" \
    --data-binary @-

wait_for_job "$JOB_GUID"
curl -s -S -f "https://api.docudevs.ai/job/result/$JOB_GUID/json" \
  -H "Authorization: $API_KEY" | jq .
# docs:end
