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

JOB_GUID=$(curl -s -S -X POST "https://api.docudevs.ai/document/upload" \
  -H "Authorization: $API_KEY" \
  -F "document=@invoice.pdf" | jq -r '.guid')

# Process with tracing enabled
curl -s -S -f -o /dev/null -X POST "https://api.docudevs.ai/document/process/$JOB_GUID" \
  -H "Authorization: $API_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "prompt": "Extract invoice data",
    "mimeType": "application/pdf",
    "trace": true
  }'

wait_for_job "$JOB_GUID"

# Retrieve the trace after the job completes
TRACE=$(curl -s -S -f "https://api.docudevs.ai/job/trace/$JOB_GUID" \
  -H "Authorization: $API_KEY")
echo "$TRACE" | jq -r '"Total tokens used: \(.total_tokens)", "LLM calls: \(.total_llm_calls)"'
# docs:end
