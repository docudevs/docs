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

CASE_ID=42  # id of a records case
VERSION_ID=7  # treeVersionId returned when the tree was uploaded
JOB_GUID="<job-guid>"  # jobGuid returned when the version was activated

wait_for_job "$JOB_GUID"

# Find the activation among the case's imports (newest first). This reads one page
# of up to 200; an older activation may need the continuation token of the page.
IMPORT_ID=$(curl -s -S -f "https://api.docudevs.ai/cases/$CASE_ID/records/imports?limit=200" \
  -H "Authorization: $API_KEY" \
  | jq -r --arg guid "$JOB_GUID" 'first((.items // [])[] | select(.jobGuid == $guid) | .importId) // empty')

if [ -z "$IMPORT_ID" ]; then
  echo "No import found for job $JOB_GUID" >&2
  exit 1
fi

curl -s -S -f "https://api.docudevs.ai/cases/$CASE_ID/trees/$VERSION_ID/activations/$IMPORT_ID/report" \
  -H "Authorization: $API_KEY" \
  | jq '{status, counts, memoScopeIssues: (.memoScopeIssues // [] | length)}'
# docs:end
