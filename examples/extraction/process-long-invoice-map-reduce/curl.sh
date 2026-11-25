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

JOB_ID=$(curl -s -S -f -X POST "https://api.docudevs.ai/document/upload" \
  -H "Authorization: $API_KEY" \
  -F "document=@large-invoice.pdf" | jq -r '.guid')

curl -s -S -f -o /dev/null -X POST "https://api.docudevs.ai/document/process/$JOB_ID" \
  -H "Authorization: $API_KEY" \
  -H "Content-Type: application/json" \
  --data-binary @- <<'JSON'
{
  "mimeType": "application/pdf",
  "prompt": "Extract invoice line items (sku, description, quantity, unitPrice, total).",
  "mapReduce": {
    "enabled": true,
    "splitType": "PAGE",
    "pagesPerChunk": 4,
    "overlapPages": 1,
    "dedupKey": "sku",
    "parallelProcessing": true,
    "header": {
      "enabled": true,
      "pageLimit": 1,
      "includeInRows": false,
      "rowPromptAugmentation": "This is an invoice from Acme Corp.",
      "schema": "{\"type\": \"object\", \"properties\": {\"invoiceNumber\": {\"type\": \"string\"}, \"issueDate\": {\"type\": \"string\"}, \"billingAddress\": {\"type\": \"string\"}}}"
    }
  }
}
JSON
echo "Job submitted: $JOB_ID"

wait_for_job "$JOB_ID"

RESULT=$(curl -s -S -f "https://api.docudevs.ai/job/result/$JOB_ID/json" \
  -H "Authorization: $API_KEY")
echo "Header: $(jq -c '.header' <<< "$RESULT")"
echo "Extracted $(jq '.records | length' <<< "$RESULT") line items."
# docs:end
