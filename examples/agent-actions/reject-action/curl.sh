#!/usr/bin/env bash
set -euo pipefail

# docs:start
ACTION_ID=9002  # id of a pending action from the case inbox

curl -s -S -f -X POST "https://api.docudevs.ai/agent/actions/$ACTION_ID/reject" \
  -H "Authorization: $API_KEY" \
  -H "Content-Type: application/json" \
  -d '{"reason": "Use the 7.31 subtree instead"}' | jq '{id, status}'
# docs:end
