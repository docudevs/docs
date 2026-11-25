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

OCR_JOB_ID=$(curl -s -S -X POST "https://api.docudevs.ai/document/upload" \
  -H "Authorization: $DOCUDEVS_API_KEY" \
  -F "document=@your-document.pdf" | jq -r '.guid')

curl -s -S -f -o /dev/null -X POST "https://api.docudevs.ai/document/ocr/$OCR_JOB_ID?format=markdown" \
  -H "Authorization: $DOCUDEVS_API_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "ocr": "PREMIUM",
    "ocrFormat": "markdown",
    "mimeType": "application/pdf"
  }'
wait_for_job "$OCR_JOB_ID"

ANALYSIS_JOB_ID=$(curl -s -S -X POST "https://api.docudevs.ai/operation/$OCR_JOB_ID/generative-task" \
  -H "Authorization: $DOCUDEVS_API_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "prompt": "Summarize this document and explain what type of document it is and its main purpose."
  }' | jq -r '.jobGuid')
wait_for_job "$ANALYSIS_JOB_ID"

echo "AI Analysis:"
curl -s -S "https://api.docudevs.ai/job/result/$ANALYSIS_JOB_ID" \
  -H "Authorization: $DOCUDEVS_API_KEY" | jq -r '.generated_text'
# docs:end
