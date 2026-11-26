#!/usr/bin/env bash
set -euo pipefail

# docs:start
curl -s -S -f "https://api.docudevs.ai/members" \
  -H "Authorization: $API_KEY" \
  | jq -r '.[]? | "\(.id)\t\(.email)\t\(.role)\t\(.status)"'
# docs:end
