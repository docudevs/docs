#!/usr/bin/env bash
set -euo pipefail

# docs:start
CASE_ID=42  # id of a records case
IMPORT_ID=301  # id of the import that failed

curl -s -S -f -X POST "https://api.docudevs.ai/cases/$CASE_ID/records/imports/$IMPORT_ID/retry" \
  -H "Authorization: $API_KEY" | jq '{importId, status}'
# docs:end
