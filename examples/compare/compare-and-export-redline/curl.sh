#!/usr/bin/env bash
set -euo pipefail

# docs:start
wait_for_status() {
  local status_url="$1"
  while true; do
    STATUS=$(curl "$status_url" -s -S -f -H "Authorization: $API_KEY" | jq -r '.status')
    case "$STATUS" in
      COMPLETED) return 0 ;;
      ERROR) echo "$status_url ended with status ERROR" >&2; return 1 ;;
    esac
    sleep 5
  done
}

COMPARE_GUID=$(curl -s -S -f -X POST "https://api.docudevs.ai/compare" \
  -H "Authorization: $API_KEY" \
  -F "original=@slip-2025.pdf" \
  -F "revised=@slip-2026-draft.pdf" \
  -F 'metadata={"originalLabel": "Slip 2025", "revisedLabel": "Draft 2026", "ocr": "PREMIUM"}' | jq -r '.compareGuid')

wait_for_status "https://api.docudevs.ai/compare/$COMPARE_GUID"

# Export a redline as PDF with only the substantive changes
EXPORT_GUID=$(curl -s -S -f -X POST "https://api.docudevs.ai/compare/$COMPARE_GUID/exports" \
  -H "Authorization: $API_KEY" \
  -H "Content-Type: application/json" \
  -d '{"format": "pdf", "filters": {"substantiveOnly": true}}' | jq -r '.exportGuid')

wait_for_status "https://api.docudevs.ai/compare/$COMPARE_GUID/exports/$EXPORT_GUID"

curl -s -S -f -o redline.pdf "https://api.docudevs.ai/compare/$COMPARE_GUID/exports/$EXPORT_GUID/file" \
  -H "Authorization: $API_KEY"
# docs:end
