#!/usr/bin/env bash
set -euo pipefail

# docs:start
PARENT_JOB_GUID="<job-guid>"  # GUID of a completed PDF job whose page images exist (processed with ocr PREMIUM)

curl -s -S -f -X POST "https://api.docudevs.ai/operation" \
  -H "Authorization: $API_KEY" \
  -H "Content-Type: application/json" \
  -d "{
    \"jobGuid\": \"$PARENT_JOB_GUID\",
    \"type\": \"pdf-acroform\",
    \"parameters\": {
      \"llmType\": \"DEFAULT\",
      \"customParameters\": {
        \"ocr\": \"PREMIUM\",
        \"fieldNameStyle\": \"source_snake_case\"
      }
    }
  }" | jq '{jobGuid, status}'
# docs:end
