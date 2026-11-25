#!/usr/bin/env bash
set -euo pipefail

# docs:start
CASE_ID=42  # id of a records case

curl -s -S -f -X DELETE "https://api.docudevs.ai/cases/$CASE_ID/records/1000000000001" \
  -H "Authorization: $API_KEY" | jq -r '"Import \(.importId): \(.status)"'
# docs:end
