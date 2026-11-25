#!/usr/bin/env bash
set -euo pipefail

# docs:start
RUN_ID="<run-id>"  # runId returned when the run was created

curl -s -S -f "https://api.docudevs.ai/contract-analysis/runs/$RUN_ID/reviews" \
  -H "Authorization: $API_KEY" | jq .
# docs:end
