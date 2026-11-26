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

JOB_ID=$(curl -s -S -f -X POST "https://api.docudevs.ai/document/upload" \
  -H "Authorization: $API_KEY" \
  -F "document=@report.pdf" | jq -r '.guid')

# extractFigures keeps the figures as images; PREMIUM OCR also renders page images
curl -s -S -f -o /dev/null -X POST "https://api.docudevs.ai/document/ocr/$JOB_ID?format=markdown" \
  -H "Authorization: $API_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "ocr": "PREMIUM",
    "mimeType": "application/pdf",
    "extractFigures": true
  }'

wait_for_job "$JOB_ID"

# List the figures, then download each one
curl -s -S -f "https://api.docudevs.ai/job/figures/$JOB_ID" \
  -H "Authorization: $API_KEY" \
  | jq -r '(.images // [])[] | "\(.id) page \(.pageIndex // "unknown")"'

curl -s -S -f "https://api.docudevs.ai/job/figures/$JOB_ID" \
  -H "Authorization: $API_KEY" \
  | jq -r '(.images // [])[].id' \
  | while read -r FIGURE_ID; do
      curl -s -S -f "https://api.docudevs.ai/job/figure/$JOB_ID/$FIGURE_ID" \
        -H "Authorization: $API_KEY" \
        --output "$FIGURE_ID.png"
    done

# Download the image of the first page (page indexes start at 0)
curl -s -S -f "https://api.docudevs.ai/job/image/$JOB_ID/0" \
  -H "Authorization: $API_KEY" \
  --output page_0.png
# docs:end
