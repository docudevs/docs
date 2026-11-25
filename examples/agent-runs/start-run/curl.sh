#!/usr/bin/env bash
set -euo pipefail

# docs:start
CASE_ID=42  # id returned when the case was created
CLASSIFY_JOB_ID=55  # id of a completed classify job of this case

wait_for_run() {
  local deadline=$((SECONDS + 1800))  # same limit as the Python timeout
  while [ "$SECONDS" -lt "$deadline" ]; do
    STATUS=$(curl -s -S -f "https://api.docudevs.ai/cases/$CASE_ID/agent/runs/$1" \
      -H "Authorization: $API_KEY" | jq -r '.status')
    case "$STATUS" in
      COMPLETED) return 0 ;;
      FAILED|CANCELLED) echo "Run $1 ended with status $STATUS" >&2; return 1 ;;
    esac
    sleep 5
  done
  echo "Run $1 did not complete in time" >&2
  return 1
}

REQUEST=$(jq -n --argjson jobId "$CLASSIFY_JOB_ID" '{
  playbook: "investigate-flagged",
  params: {jobId: $jobId, maxRows: 10},
  budget: {maxTotalTokens: 60000}
}')

RUN_ID=$(curl -s -S -f -X POST "https://api.docudevs.ai/cases/$CASE_ID/agent/runs" \
  -H "Authorization: $API_KEY" \
  -H "Content-Type: application/json" \
  -d "$REQUEST" | jq -r '.runId')

wait_for_run "$RUN_ID"

curl -s -S -f "https://api.docudevs.ai/cases/$CASE_ID/agent/runs/$RUN_ID" \
  -H "Authorization: $API_KEY" | jq '{summary: .report.summary, questions: (.report.questions // [])}'
# docs:end
