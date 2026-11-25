#!/usr/bin/env bash
set -euo pipefail

# docs:start
CASE_ID=42  # id returned when the case was created
CLASSIFY_JOB_ID=55  # id of a classify job of this case

curl -s -S -f "https://api.docudevs.ai/cases/$CASE_ID/agent/runs?originJobId=$CLASSIFY_JOB_ID&status=COMPLETED" \
  -H "Authorization: $API_KEY" | jq -r '.items[] | "\(.runId) \(.status) \(.tokensUsed)"'
# docs:end
