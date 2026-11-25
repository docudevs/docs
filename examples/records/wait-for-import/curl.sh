#!/usr/bin/env bash
set -euo pipefail

# docs:start
CASE_ID=42  # id of a records case
IMPORT_ID=301  # importId returned when the import was created

DEADLINE=$((SECONDS + 1800))  # same limit as the Python timeout
while [ "$SECONDS" -lt "$DEADLINE" ]; do
  IMPORT=$(curl -s -S -f "https://api.docudevs.ai/cases/$CASE_ID/records/imports/$IMPORT_ID" \
    -H "Authorization: $API_KEY")
  STATUS=$(echo "$IMPORT" | jq -r '.status')
  case "$STATUS" in
    COMPLETED) echo "$IMPORT" | jq '{status, recordsAdded, recordsUpdated}'; exit 0 ;;
    FAILED) echo "Import $IMPORT_ID failed" >&2; exit 1 ;;
  esac
  sleep 2
done
echo "Import $IMPORT_ID did not complete in time" >&2
exit 1
# docs:end
