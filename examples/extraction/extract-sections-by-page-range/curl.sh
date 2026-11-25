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

# Upload one document per extraction and process only the pages that hold the section
extract_pages() {
  local pages="$1" prompt="$2" job_guid
  job_guid=$(curl -s -S -f -X POST "https://api.docudevs.ai/document/upload" \
    -H "Authorization: $API_KEY" \
    -F "document=@safety-data-sheet.pdf" | jq -r '.guid')

  jq -n --argjson pages "$pages" --arg prompt "$prompt" \
    '{mimeType: "application/pdf", prompt: $prompt, pageRange: $pages}' |
    curl -s -S -f -o /dev/null -X POST "https://api.docudevs.ai/document/process/$job_guid" \
      -H "Authorization: $API_KEY" \
      -H "Content-Type: application/json" \
      --data-binary @-

  wait_for_job "$job_guid"
  curl -s -S -f "https://api.docudevs.ai/job/result/$job_guid/json" \
    -H "Authorization: $API_KEY" | jq .
}

# Section 1: Identification (page 1)
extract_pages '[1]' "Extract product name, manufacturer, and emergency contact."

# Section 9: Physical properties (pages 8-9)
extract_pages '[8, 9]' "Extract appearance, odor, boiling point, and flash point."

# Sections 14-16: Transport and regulatory (pages 12-14)
extract_pages '[12, 13, 14]' "Extract transport classification and regulatory information."
# docs:end
