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

BATCH_GUID=$(curl -s -S -f -X POST "https://api.docudevs.ai/document/batch" \
  -H "Authorization: $API_KEY" \
  -H "Content-Type: application/json" \
  -d '{"maxConcurrency": 3}' | jq -r '.jobGuid')

for FILENAME in invoice1.pdf invoice2.pdf invoice3.pdf; do
  curl -s -S -f -o /dev/null -X POST "https://api.docudevs.ai/document/batch/$BATCH_GUID/upload" \
    -H "Authorization: $API_KEY" \
    -F "document=@$FILENAME"
done

SCHEMA='{
  "type": "object",
  "properties": {
    "invoice_number": {"type": "string"},
    "total": {"type": "number"}
  },
  "required": ["total"]
}'

jq -n \
  --arg schema "$SCHEMA" \
  '{
    mimeType: "application/pdf",
    prompt: "Extract invoice data",
    schema: $schema
  }' |
  curl -s -S -f -o /dev/null -X POST "https://api.docudevs.ai/document/batch/$BATCH_GUID/process" \
    -H "Authorization: $API_KEY" \
    -H "Content-Type: application/json" \
    --data-binary @-

wait_for_job "$BATCH_GUID"

# A document that failed or has no stored result is null in this list
curl -s -S -f "https://api.docudevs.ai/job/result/$BATCH_GUID/json" \
  -H "Authorization: $API_KEY" | jq .
# docs:end
