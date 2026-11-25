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

PARENT_JOB_GUID="<job-guid>"  # GUID of a completed PDF job whose page images exist (processed with ocr PREMIUM)

OPERATION_JOB_GUID=$(curl -s -S -f -X POST "https://api.docudevs.ai/operation" \
  -H "Authorization: $API_KEY" \
  -H "Content-Type: application/json" \
  -d "{
    \"jobGuid\": \"$PARENT_JOB_GUID\",
    \"type\": \"pdf-acroform\",
    \"parameters\": {
      \"llmType\": \"DEFAULT\",
      \"customParameters\": {
        \"ocr\": \"PREMIUM\",
        \"fieldNameStyle\": \"source_snake_case\",
        \"pageRange\": [1],
        \"minConfidence\": 0.6,
        \"maxFieldsPerPage\": 300
      }
    }
  }" | jq -r '.jobGuid')

wait_for_job "$OPERATION_JOB_GUID"

curl -s -S -f "https://api.docudevs.ai/job/result/$OPERATION_JOB_GUID/pdf-acroform" \
  -H "Authorization: $API_KEY" \
  --output fillable.pdf

echo "Operation job GUID: $OPERATION_JOB_GUID"
# docs:end
