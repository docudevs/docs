#!/usr/bin/env bash
set -euo pipefail

# docs:start
RUN_ID="<run-id>"  # runId returned when the run was created

curl -s -S "https://api.docudevs.ai/contract-analysis/runs/$RUN_ID/result" \
  -H "Authorization: $DOCUDEVS_API_KEY" | jq .
# docs:end
