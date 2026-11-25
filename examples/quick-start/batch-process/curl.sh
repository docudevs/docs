#!/usr/bin/env bash
set -euo pipefail

# docs:start
wait_for_job() {
  while true; do
    STATUS=$(curl -s -S "https://api.docudevs.ai/job/status/$1" \
      -H "Authorization: $DOCUDEVS_API_KEY" | jq -r '.status')
    if [ "$STATUS" = "COMPLETED" ]; then return 0; fi
    if [ "$STATUS" = "ERROR" ]; then echo "Job $1 failed" >&2; return 1; fi
    sleep 5
  done
}

BATCH_GUID=$(curl -s -S -X POST "https://api.docudevs.ai/document/batch" \
  -H "Authorization: $DOCUDEVS_API_KEY" \
  -H "Content-Type: application/json" \
  -d '{"maxConcurrency": 3}' | jq -r '.jobGuid')

for FILENAME in invoice1.pdf invoice2.pdf invoice3.pdf; do
  curl -s -S -f -o /dev/null -X POST "https://api.docudevs.ai/document/batch/$BATCH_GUID/upload" \
    -H "Authorization: $DOCUDEVS_API_KEY" \
    -F "document=@$FILENAME"
done

SCHEMA='{
  "type": "object",
  "properties": {
    "invoice_number": {"type": "string"},
    "total": {"type": "number"}
  }
}'

jq -n \
  --arg schema "$SCHEMA" \
  '{
    mimeType: "application/pdf",
    prompt: "Extract invoice data",
    schema: $schema
  }' |
  curl -s -S -f -o /dev/null -X POST "https://api.docudevs.ai/document/batch/$BATCH_GUID/process" \
    -H "Authorization: $DOCUDEVS_API_KEY" \
    -H "Content-Type: application/json" \
    --data-binary @-

wait_for_job "$BATCH_GUID"

curl -s -S "https://api.docudevs.ai/job/result/$BATCH_GUID/json" \
  -H "Authorization: $DOCUDEVS_API_KEY" | jq .
# docs:end
