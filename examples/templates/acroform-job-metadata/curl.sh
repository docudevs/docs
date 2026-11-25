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

JOB_ID=$(curl -s -S -X POST "https://api.docudevs.ai/document/upload" \
  -H "Authorization: $API_KEY" \
  -F "document=@fillable-form.pdf" | jq -r '.guid')

curl -s -S -f -o /dev/null -X POST "https://api.docudevs.ai/document/process/$JOB_ID" \
  -H "Authorization: $API_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "mimeType": "application/pdf",
    "acroFormMetadata": true,
    "sourceLocations": true,
    "sourceLocationGranularity": "BLOCK"
  }'

wait_for_job "$JOB_ID"

curl -s -S -X GET "https://api.docudevs.ai/job/result/$JOB_ID/acroform-metadata" \
  -H "Authorization: $API_KEY" | jq '.fields | length'

curl -s -S -X GET "https://api.docudevs.ai/job/result/$JOB_ID/source-locations" \
  -H "Authorization: $API_KEY" | jq .
# docs:end
