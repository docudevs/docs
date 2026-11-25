#!/usr/bin/env bash
set -euo pipefail

# docs:start
CASE_ID=42  # id of a records case with a configured dataset
FILE=snapshot.ndjson  # the full export, with columns as named in the dataset field mapping

wait_for_import() {
  local deadline=$((SECONDS + 1800))  # same limit as the Python timeout
  while [ "$SECONDS" -lt "$deadline" ]; do
    IMPORT=$(curl -s -S -f "https://api.docudevs.ai/cases/$CASE_ID/records/imports/$1" \
      -H "Authorization: $API_KEY")
    case "$(echo "$IMPORT" | jq -r '.status')" in
      COMPLETED) return 0 ;;
      FAILED) echo "Import $1 failed" >&2; return 1 ;;
    esac
    sleep 2
  done
  echo "Import $1 did not complete in time" >&2
  return 1
}

# Creates the import from the options in $1, uploads the file in blocks of at
# most 32 MiB, commits it and waits for it
sync_file() {
  IMPORT_ID=$(curl -s -S -f -X POST "https://api.docudevs.ai/cases/$CASE_ID/records/imports" \
    -H "Authorization: $API_KEY" \
    -H "Content-Type: application/json" \
    -d "$1" | jq -r '.importId')
  rm -f block-*
  split -b 32m "$FILE.gz" block-
  BLOCKS=0
  for BLOCK in block-*; do
    curl -s -S -f -o /dev/null -X PUT "https://api.docudevs.ai/cases/$CASE_ID/records/imports/$IMPORT_ID/upload/blocks/$BLOCKS" \
      -H "Authorization: $API_KEY" \
      -H "Content-Type: application/octet-stream" \
      --data-binary @"$BLOCK"
    BLOCKS=$((BLOCKS + 1))
  done
  curl -s -S -f -o /dev/null -X POST "https://api.docudevs.ai/cases/$CASE_ID/records/imports/$IMPORT_ID/commit?blockCount=$BLOCKS" \
    -H "Authorization: $API_KEY"
  wait_for_import "$IMPORT_ID"
}

gzip -k -f "$FILE"

sync_file '{"mode": "sync", "format": "ndjson", "gzip": true, "dryRun": true}'
DRY_RUN_ID=$IMPORT_ID
echo "$IMPORT" | jq '.syncSummary | {counts, guard}'

# After reviewing the report, apply the same file for real
sync_file '{"mode": "sync", "format": "ndjson", "gzip": true, "dryRun": false, "reportId": "'"$DRY_RUN_ID"'"}'
echo "$IMPORT" | jq '{status, recordsAdded, recordsUpdated, recordsDeleted, recordsUnchanged}'
# docs:end
