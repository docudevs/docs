#!/usr/bin/env bash
set -euo pipefail

# docs:start
# EXECUTION_ID is the "executionId" returned when the run was started.
EXECUTION_ID="managed-exec-00000000-0000-0000-0000-000000000000"

curl -s -S -f "https://api.docudevs.ai/managed-execution/$EXECUTION_ID/logs" \
  -H "Authorization: $API_KEY" \
  | jq -r '.[] | [(.timestamp // ""), (.level // ""), (.taskId // ""), (.message // "")] | @tsv'
# docs:end
