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
      {"role": "user", "content": "Help me build an invoice extraction schema"}
    ],
    "sessionId": "session-123"
  }' | jq -r '.jobGuid')

wait_for_chat "$JOB_GUID"

curl -s -S -f "https://api.docudevs.ai/agent/status/$JOB_GUID" \
  -H "Authorization: $API_KEY" | jq -r '.response.message // empty'
# docs:end
