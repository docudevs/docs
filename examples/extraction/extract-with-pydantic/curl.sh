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

# The JSON Schema for the Invoice model
SCHEMA='{
  "$defs": {
    "LineItem": {
      "properties": {
        "description": {"title": "Description", "type": "string"},
        "quantity": {"title": "Quantity", "type": "integer"},
        "price": {"title": "Price", "type": "number"}
      },
      "required": ["description", "quantity", "price"],
      "title": "LineItem",
      "type": "object"
    }
  },
  "properties": {
    "number": {"description": "The invoice number", "title": "Number", "type": "string"},
    "date": {"title": "Date", "type": "string"},
    "items": {"items": {"$ref": "#/$defs/LineItem"}, "title": "Items", "type": "array"},
    "total": {"title": "Total", "type": "number"}
  },
  "required": ["number", "date", "items", "total"],
  "title": "Invoice",
  "type": "object"
}'

JOB_ID=$(curl -s -S -X POST "https://api.docudevs.ai/document/upload" \
  -H "Authorization: $API_KEY" \
  -F "document=@invoice.pdf" | jq -r '.guid')

jq -n \
  --arg schema "$SCHEMA" \
  '{mimeType: "application/pdf", schema: $schema}' |
  curl -s -S -f -o /dev/null -X POST "https://api.docudevs.ai/document/process/$JOB_ID" \
    -H "Authorization: $API_KEY" \
    -H "Content-Type: application/json" \
    --data-binary @-

wait_for_job "$JOB_ID"

curl -s -S "https://api.docudevs.ai/job/result/$JOB_ID/json" \
  -H "Authorization: $API_KEY" | jq .
# docs:end
