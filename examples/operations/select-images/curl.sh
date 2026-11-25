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

JOB_GUID="<job-guid>"  # GUID of a completed job processed with figure extraction

OPERATION_JOB_GUID=$(curl -s -S -f -X POST "https://api.docudevs.ai/operation" \
  -H "Authorization: $API_KEY" \
  -H "Content-Type: application/json" \
  -d "{
        \"jobGuid\": \"$JOB_GUID\",
        \"type\": \"image-select\",
        \"parameters\": {
          \"customParameters\": {
            \"prompt\": \"Return all diagrams from the document\",
            \"topK\": 5,
            \"matchMode\": \"all\",
            \"useVision\": false
          }
        }
      }" | jq -r '.jobGuid')

wait_for_job "$OPERATION_JOB_GUID"

PAYLOAD=$(curl -s -S -f "https://api.docudevs.ai/job/result/$OPERATION_JOB_GUID" \
  -H "Authorization: $API_KEY")
echo "$PAYLOAD" | jq .

IMAGE_ID=$(echo "$PAYLOAD" | jq -r '.selected[0].id // empty')
if [ -n "$IMAGE_ID" ]; then
  curl -s -S -f "https://api.docudevs.ai/job/figure/$JOB_GUID/$IMAGE_ID" \
    -H "Authorization: $API_KEY" \
    --output selected_figure.png
fi
# docs:end
