#!/usr/bin/env bash
set -euo pipefail

# docs:start
wait_for_job() {
  while true; do
    STATUS=$(curl -s -S -f "https://api.docudevs.ai/job/status/$1" \
      -H "Authorization: $API_KEY" | jq -r '.status')
    case "$STATUS" in
      COMPLETED) return 0 ;;
      ERROR|FAILED|TIMEOUT|PARTIAL) echo "Job $1 ended with status $STATUS" >&2; return 1 ;;
    esac
    sleep 2
  done
}

PARENT_JOB_GUID="<job-guid>"  # GUID of a completed document job

QUESTIONS=(
  "What is the contract duration?"
  "Who are the parties involved?"
  "What are the key obligations?"
)

for QUESTION in "${QUESTIONS[@]}"; do
  BODY=$(jq -n --arg question "$QUESTION" \
    '{prompt: "Based on this document, answer the following question: \($question)"}')

  TASK_JOB_GUID=$(curl -s -S -f -X POST "https://api.docudevs.ai/operation/$PARENT_JOB_GUID/generative-task" \
    -H "Authorization: $API_KEY" \
    -H "Content-Type: application/json" \
    -d "$BODY" | jq -r '.jobGuid')

  wait_for_job "$TASK_JOB_GUID"

  ANSWER=$(curl -s -S -f "https://api.docudevs.ai/job/result/$TASK_JOB_GUID" \
    -H "Authorization: $API_KEY" | jq -r '.result | fromjson | .generated_text')
  echo "Q: $QUESTION"
  echo "A: $ANSWER"
  echo
done
# docs:end
