#!/usr/bin/env bash
set -euo pipefail

# docs:start
CASE_ID=42  # id of a records case
IMPORT_ID=301  # importId returned when the import was created

curl -s -S -f "https://api.docudevs.ai/cases/$CASE_ID/records/imports/$IMPORT_ID" \
  -H "Authorization: $API_KEY" | jq '{status, rowsReceived, rowsRejected, recordsAdded, recordsUpdated, recordsDeleted, recordsUnchanged}'
# docs:end
