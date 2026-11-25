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
  -F "document=@handwritten.pdf" | jq -r '.guid')

# ocr: DEFAULT, PREMIUM or AUTO. format: markdown or plain
curl -s -S -f -o /dev/null -X POST "https://api.docudevs.ai/document/ocr/$JOB_ID?format=plain" \
  -H "Authorization: $API_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "ocr": "PREMIUM",
    "ocrFormat": "plain",
    "mimeType": "application/pdf"
  }'

wait_for_job "$JOB_ID"

curl -s -S "https://api.docudevs.ai/job/result/$JOB_ID" \
  -H "Authorization: $API_KEY"
# docs:end
