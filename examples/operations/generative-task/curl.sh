#!/usr/bin/env bash
set -euo pipefail

# docs:start
wait_for_job() {
  while true; do
    STATUS=$(curl -s -S "https://api.docudevs.ai/job/status/$1" \
      -H "Authorization: $API_KEY" | jq -r '.status')
    if [ "$STATUS" = "COMPLETED" ]; then return 0; fi
    if [ "$STATUS" = "ERROR" ]; then echo "Job $1 failed" >&2; return 1; fi
    sleep 2
  done
}

PARENT_JOB_GUID="<job-guid>"  # GUID of a completed document job

TASK_JOB_GUID=$(curl -s -S -f -X POST "https://api.docudevs.ai/operation/$PARENT_JOB_GUID/generative-task" \
  -H "Authorization: $API_KEY" \
  -H "Content-Type: application/json" \
  -d '{"prompt": "Summarize the main findings", "model": "DEFAULT"}' | jq -r '.jobGuid')

wait_for_job "$TASK_JOB_GUID"

# The result is a JSON string inside the response object
curl -s -S -f "https://api.docudevs.ai/operation/$PARENT_JOB_GUID/generative-task" \
  -H "Authorization: $API_KEY" | jq -r '.result | fromjson | .generated_text'
# docs:end
