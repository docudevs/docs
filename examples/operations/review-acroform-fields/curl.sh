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

PARENT_JOB_GUID="<job-guid>"  # GUID of the completed PDF job that was converted
OPERATION_JOB_GUID="<operation-job-guid>"  # operation job GUID returned by the conversion

curl -s -S -f "https://api.docudevs.ai/job/result/$OPERATION_JOB_GUID/acroform-metadata" \
  -H "Authorization: $API_KEY" | jq '.fields[:3]'

DEFINITIONS=$(curl -s -S -f "https://api.docudevs.ai/job/result/$OPERATION_JOB_GUID/pdf-acroform-field-definitions" \
  -H "Authorization: $API_KEY")
echo "$DEFINITIONS" | jq '.fieldDefinitions[:3]'

# Mark every generated field as reviewed and accepted
REVIEWED=$(echo "$DEFINITIONS" | jq '.fieldDefinitions | map(. + {reviewStatus: "accepted"})')

APPLY_JOB_GUID=$(jq -n \
  --arg source "$OPERATION_JOB_GUID" \
  --argjson fields "$REVIEWED" \
  '{sourceOperationGuid: $source, fieldDefinitions: $fields, minConfidence: 0.0, maxFieldsPerPage: 300}' |
  curl -s -S -f -X POST "https://api.docudevs.ai/job/$PARENT_JOB_GUID/pdf-acroform/apply" \
    -H "Authorization: $API_KEY" \
    -H "Content-Type: application/json" \
    -d @- | jq -r '.jobGuid')

wait_for_job "$APPLY_JOB_GUID"

curl -s -S -f "https://api.docudevs.ai/job/result/$APPLY_JOB_GUID/pdf-acroform" \
  -H "Authorization: $API_KEY" \
  --output reviewed.pdf

echo "Operation job GUID: $APPLY_JOB_GUID"
# docs:end
