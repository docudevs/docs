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

OCR_JOB_ID=$(curl -s -S -f -X POST "https://api.docudevs.ai/document/upload" \
  -H "Authorization: $API_KEY" \
  -F "document=@report.pdf" | jq -r '.guid')

curl -s -S -f -o /dev/null -X POST "https://api.docudevs.ai/document/ocr/$OCR_JOB_ID?format=markdown" \
  -H "Authorization: $API_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "ocr": "PREMIUM",
    "ocrFormat": "markdown",
    "mimeType": "application/pdf"
  }'
wait_for_job "$OCR_JOB_ID"

NAMES=(document_type key_information summary tone)
PROMPTS=(
  "What type of document is this? (invoice, contract, report, form, etc.)"
  "Extract the most important information: dates, names, amounts, addresses, and key terms."
  "Provide a concise summary of this document's main content and purpose."
  "Analyze the tone of this document. Is it formal, informal, positive, negative, urgent?"
)

RESULTS='{}'
for INDEX in "${!NAMES[@]}"; do
  BODY=$(jq -n --arg prompt "${PROMPTS[$INDEX]}" '{prompt: $prompt}')

  TASK_JOB_GUID=$(curl -s -S -f -X POST "https://api.docudevs.ai/operation/$OCR_JOB_ID/generative-task" \
    -H "Authorization: $API_KEY" \
    -H "Content-Type: application/json" \
    -d "$BODY" | jq -r '.jobGuid')
  wait_for_job "$TASK_JOB_GUID"

  TEXT=$(curl -s -S -f "https://api.docudevs.ai/job/result/$TASK_JOB_GUID" \
    -H "Authorization: $API_KEY" | jq -r '.generated_text')
  RESULTS=$(jq --arg name "${NAMES[$INDEX]}" --arg text "$TEXT" '. + {($name): $text}' <<< "$RESULTS")
done

echo "$RESULTS" | jq .
# docs:end
