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

INSTRUCTIONS="Focus on extracting the invoice number, date, and all line items with prices."

INSTRUCTIONS_QUERY=$(jq -rn --arg text "$INSTRUCTIONS" '$text | @uri')

JOB_GUID=$(curl -s -S -f -X POST "https://api.docudevs.ai/document/generate-schema?instructionsText=$INSTRUCTIONS_QUERY" \
  -H "Authorization: $API_KEY" \
  -F "document=@sample-invoice.pdf" | jq -r '.guid')

wait_for_job "$JOB_GUID"

echo "Generated schema:"
curl -s -S -f "https://api.docudevs.ai/job/result/$JOB_GUID/json" \
  -H "Authorization: $API_KEY" | jq .
# docs:end
