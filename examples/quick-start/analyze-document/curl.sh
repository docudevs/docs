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
  -F "document=@your-document.pdf" | jq -r '.guid')

curl -s -S -f -o /dev/null -X POST "https://api.docudevs.ai/document/ocr/$OCR_JOB_ID?format=markdown" \
  -H "Authorization: $API_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "ocr": "PREMIUM",
    "ocrFormat": "markdown",
    "mimeType": "application/pdf"
  }'
wait_for_job "$OCR_JOB_ID"

ANALYSIS_JOB_ID=$(curl -s -S -f -X POST "https://api.docudevs.ai/operation/$OCR_JOB_ID/generative-task" \
  -H "Authorization: $API_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "prompt": "Summarize this document and explain what type of document it is and its main purpose."
  }' | jq -r '.jobGuid')
wait_for_job "$ANALYSIS_JOB_ID"

echo "AI Analysis:"
curl -s -S -f "https://api.docudevs.ai/job/result/$ANALYSIS_JOB_ID" \
  -H "Authorization: $API_KEY" | jq -r '.generated_text'
# docs:end
