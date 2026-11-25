#!/usr/bin/env bash
set -euo pipefail

# docs:start
CASE_ID=42  # id returned when the case was created
RUN_ID=9001  # id returned when the run was started

curl -s -S -f -X POST "https://api.docudevs.ai/cases/$CASE_ID/agent/runs/$RUN_ID/cancel" \
  -H "Authorization: $API_KEY" | jq '{runId, status}'
# docs:end
