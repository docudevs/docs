#!/usr/bin/env bash
set -euo pipefail

# docs:start
wait_for_chat() {
  local deadline=$((SECONDS + 120))  # same limit as the Python timeout
  while [ "$SECONDS" -lt "$deadline" ]; do
    STATUS=$(curl -s -S -f "https://api.docudevs.ai/agent/status/$1" \
      -H "Authorization: $API_KEY" | jq -r '.status')
    case "$STATUS" in
      COMPLETED) return 0 ;;
      ERROR|FAILED) echo "Chat $1 ended with status $STATUS" >&2; return 1 ;;
    esac
    sleep 2
  done
  echo "Chat $1 did not complete in time" >&2
  return 1
}

JOB_GUID=$(curl -s -S -f -X POST "https://api.docudevs.ai/agent/chat" \
  -H "Authorization: $API_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "messages": [
      {"role": "user", "content": "Classify this week file into levels 4 and 5 using the memo rules."}
    ],
    "sessionId": "week-41",
    "caseId": 42,
    "playbook": "general"
  }' | jq -r '.jobGuid')

wait_for_chat "$JOB_GUID"

STATUS_JSON=$(curl -s -S -f "https://api.docudevs.ai/agent/status/$JOB_GUID" \
  -H "Authorization: $API_KEY")
echo "$STATUS_JSON" | jq -r '.response.message // empty'
echo "$STATUS_JSON" | jq -r '.pendingActions[]? | "\(.id) \(.summary)"'
# docs:end
