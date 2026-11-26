#!/usr/bin/env bash
set -euo pipefail

# docs:start
# EXECUTION_ID is the "executionId" returned when the run was started.
EXECUTION_ID="managed-exec-00000000-0000-0000-0000-000000000000"

# 200 when the cancellation is accepted, 400 when the run is already cancelling or finished
curl -s -S -X POST "https://api.docudevs.ai/managed-execution/$EXECUTION_ID/cancel" \
  -H "Authorization: $API_KEY" \
  -w "HTTP %{http_code}\n"
# docs:end
