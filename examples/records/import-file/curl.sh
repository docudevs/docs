#!/usr/bin/env bash
set -euo pipefail

# docs:start
CASE_ID=42  # id of a records case with a configured dataset
FILE=products.csv  # columns as named in the dataset field mapping

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

gzip -k -f "$FILE"

IMPORT_ID=$(curl -s -S -f -X POST "https://api.docudevs.ai/cases/$CASE_ID/records/imports" \
  -H "Authorization: $API_KEY" \
  -H "Content-Type: application/json" \
  -d '{"mode": "upsert", "format": "csv", "gzip": true}' | jq -r '.importId')

# Blocks of at most 32 MiB, numbered from 0; the file is the blocks in index order
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
echo "$IMPORT" | jq '{status, rowsReceived, rowsRejected, recordsAdded, recordsUpdated, recordsUnchanged}'
# docs:end
