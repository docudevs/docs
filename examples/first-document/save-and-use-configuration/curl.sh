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

SCHEMA='{
  "type": "object",
  "properties": {
    "invoice_number": {"type": "string"},
    "date": {"type": "string", "format": "date"},
    "vendor": {"type": "string"},
    "total": {"type": "number"}
  }
}'

# Save the configuration under a name
jq -n \
  --arg schema "$SCHEMA" \
  '{
    prompt: "Extract comprehensive invoice data",
    schema: $schema,
    llm: "HIGH"
  }' |
  curl -s -S -f -o /dev/null -X POST "https://api.docudevs.ai/configuration/invoice_processing_standard" \
    -H "Authorization: $API_KEY" \
    -H "Content-Type: application/json" \
    --data-binary @-
echo "Configuration saved: invoice_processing_standard"

# Upload a document and process it with the saved configuration
JOB_ID=$(curl -s -S -X POST "https://api.docudevs.ai/document/upload" \
  -H "Authorization: $API_KEY" \
  -F "document=@sample_invoice.pdf" | jq -r '.guid')

curl -s -S -f -o /dev/null -X POST "https://api.docudevs.ai/document/process/$JOB_ID/with-configuration/invoice_processing_standard" \
  -H "Authorization: $API_KEY"

wait_for_job "$JOB_ID"

echo "Processing with Named Configuration Result:"
curl -s -S "https://api.docudevs.ai/job/result/$JOB_ID/json" \
  -H "Authorization: $API_KEY" | jq .
# docs:end
