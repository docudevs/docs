#!/usr/bin/env bash
set -euo pipefail

# docs:start
if [ ! -f sample_invoice.pdf ]; then
  echo "Document file not found: sample_invoice.pdf" >&2
  exit 1
fi

JOB_ID=$(curl -s -S -X POST "https://api.docudevs.ai/document/upload" \
  -H "Authorization: $API_KEY" \
  -F "document=@sample_invoice.pdf" | jq -r '.guid')

curl -s -S -f -o /dev/null -X POST "https://api.docudevs.ai/document/process/$JOB_ID" \
  -H "Authorization: $API_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "mimeType": "application/pdf",
    "prompt": "Extract key information from this document"
  }'
echo "Processing job $JOB_ID..."

# Wait up to 5 minutes for the result
DEADLINE=$((SECONDS + 300))
while true; do
  STATUS=$(curl -s -S "https://api.docudevs.ai/job/status/$JOB_ID" \
    -H "Authorization: $API_KEY" | jq -r '.status')
  if [ "$STATUS" = "COMPLETED" ]; then
    echo "Processing successful!"
    curl -s -S "https://api.docudevs.ai/job/result/$JOB_ID/json" \
      -H "Authorization: $API_KEY" | jq .
    break
  fi
  if [ "$STATUS" = "ERROR" ]; then
    echo "Processing failed for job $JOB_ID" >&2
    exit 1
  fi
  if [ "$SECONDS" -ge "$DEADLINE" ]; then
    echo "Processing timed out for job $JOB_ID" >&2
    echo "Current status: $STATUS" >&2
    exit 1
  fi
  sleep 5
done
# docs:end
