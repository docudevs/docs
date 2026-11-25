#!/usr/bin/env bash
set -euo pipefail

# docs:start
CASE_ID=42  # id returned when the case was created

curl -s -S -f "https://api.docudevs.ai/cases/$CASE_ID/agent/actions?status=pending&limit=20" \
  -H "Authorization: $API_KEY" | jq -r '.items[]? | "\(.id) \(.tool) \(.summary)"'
# docs:end
