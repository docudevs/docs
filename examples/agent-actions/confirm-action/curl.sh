#!/usr/bin/env bash
set -euo pipefail

# docs:start
ACTION_ID=9001  # id of a pending action from the case inbox

RESULT=$(curl -s -S -f -X POST "https://api.docudevs.ai/agent/actions/$ACTION_ID/confirm" \
  -H "Authorization: $API_KEY")
echo "$RESULT" | jq '{status, resultStatus}'

# A write that answered an error ends as "failed", not "confirmed"
echo "$RESULT" | jq -e '.status == "confirmed"' > /dev/null
# docs:end
