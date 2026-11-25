#!/usr/bin/env bash
set -euo pipefail

# docs:start
JOB_GUID="<job-guid>"  # GUID of a completed document job

curl -s -S -f -X POST "https://api.docudevs.ai/operation" \
  -H "Authorization: $API_KEY" \
  -H "Content-Type: application/json" \
  -d "{
    \"jobGuid\": \"$JOB_GUID\",
    \"type\": \"error-analysis\",
    \"parameters\": {
      \"llmType\": \"HIGH\"
    }
  }" | jq '{jobGuid, status}'
# docs:end
