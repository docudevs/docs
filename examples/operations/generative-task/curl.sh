#!/usr/bin/env bash
set -euo pipefail

# docs:start
wait_for_job() {
  while true; do
    STATUS=$(curl -s -S -f "https://api.docudevs.ai/job/status/$1" \
      -H "Authorization: $API_KEY" | jq -r '.status')
    case "$STATUS" in
      COMPLETED) return 0 ;;
      PARTIAL) echo "Job $1 ended with status PARTIAL" >&2; return 0 ;;
      ERROR|FAILED|TIMEOUT) echo "Job $1 ended with status $STATUS" >&2; return 1 ;;
    esac
    sleep 2
  done
}

PARENT_JOB_GUID="<job-guid>"  # GUID of a completed document job

TASK_JOB_GUID=$(curl -s -S -f -X POST "https://api.docudevs.ai/operation/$PARENT_JOB_GUID/generative-task" \
  -H "Authorization: $API_KEY" \
  -H "Content-Type: application/json" \
  -d '{"prompt": "Summarize the main findings", "model": "DEFAULT"}' | jq -r '.jobGuid')

wait_for_job "$TASK_JOB_GUID"

curl -s -S -f "https://api.docudevs.ai/job/result/$TASK_JOB_GUID" \
  -H "Authorization: $API_KEY" | jq -r '.generated_text'
# docs:end
