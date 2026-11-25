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
    "registration_number": {"type": "string"},
    "first_registration_date": {"type": "string", "format": "date"},
    "vehicle_age_years": {"type": "number"}
  }
}'

JOB_GUID=$(curl -s -S -f -X POST "https://api.docudevs.ai/document/upload" \
  -H "Authorization: $API_KEY" \
  -F "document=@registration.pdf" | jq -r '.guid')

# Process with includeCurrentDate enabled
jq -n --arg schema "$SCHEMA" \
  '{
    mimeType: "application/pdf",
    prompt: "Extract vehicle registration details including vehicle age in years",
    schema: $schema,
    includeCurrentDate: true
  }' |
  curl -s -S -f -o /dev/null -X POST "https://api.docudevs.ai/document/process/$JOB_GUID" \
    -H "Authorization: $API_KEY" \
    -H "Content-Type: application/json" \
    --data-binary @-

wait_for_job "$JOB_GUID"

curl -s -S -f "https://api.docudevs.ai/job/result/$JOB_GUID/json" \
  -H "Authorization: $API_KEY" | jq .
# docs:end
