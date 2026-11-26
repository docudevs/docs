#!/usr/bin/env bash
set -euo pipefail

# docs:start
# Newest runs first. Leave out the status parameter to list every run.
curl -s -S -f "https://api.docudevs.ai/managed-execution?status=FAILED" \
  -H "Authorization: $API_KEY" \
  | jq -r '.[] | [.executionId, .flowId, .status, .triggerType, (.errorMessage // "")] | @tsv'
# docs:end
