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

JOB_GUID=$(curl -s -S -f -X POST "https://api.docudevs.ai/document/upload" \
  -H "Authorization: $API_KEY" \
  -F "document=@submission.xlsx" | jq -r '.guid')

curl -s -S -f -o /dev/null -X POST "https://api.docudevs.ai/document/process/$JOB_GUID" \
  -H "Authorization: $API_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "extractionMode": "WORKBOOK",
    "prompt": "Find related tables. Preserve source values, units, row grain, and source references.",
    "mimeType": "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
  }'

wait_for_job "$JOB_GUID"

curl -s -S -f "https://api.docudevs.ai/job/result/$JOB_GUID" \
  -H "Authorization: $API_KEY" | jq .

# The plan, source map and summary are separate artifacts of the same job
for ARTIFACT in plan source-map summary; do
  curl -s -S -f "https://api.docudevs.ai/job/result/$JOB_GUID/workbook/$ARTIFACT" \
    -H "Authorization: $API_KEY" | jq .
done
# docs:end
