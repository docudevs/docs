#!/usr/bin/env bash
set -euo pipefail

# docs:start
CASE_ID=42  # id of a records case with an active category tree
FILE=weekly.csv  # columns are the source column names of the dataset

gzip -k -f "$FILE"

JOB_ID=$(curl -s -S -f -X POST "https://api.docudevs.ai/cases/$CASE_ID/classify" \
  -H "Authorization: $API_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "rows": {"source": "upload", "format": "csv"},
    "config": {"maxTotalTokens": 5000000}
  }' | jq -r '.jobId')

# Blocks of at most 32 MiB, numbered from 0; the file is the blocks in index order
rm -f block-*
split -b 32m "$FILE.gz" block-
BLOCKS=0
for BLOCK in block-*; do
  curl -s -S -f -o /dev/null -X PUT "https://api.docudevs.ai/cases/$CASE_ID/classify/$JOB_ID/upload/blocks/$BLOCKS" \
    -H "Authorization: $API_KEY" \
    -H "Content-Type: application/octet-stream" \
    --data-binary @"$BLOCK"
  BLOCKS=$((BLOCKS + 1))
done

curl -s -S -f -X POST "https://api.docudevs.ai/cases/$CASE_ID/classify/$JOB_ID/commit?blockCount=$BLOCKS" \
  -H "Authorization: $API_KEY" | jq -r '"Classify job \(.jobId): \(.status)"'
# Read GET /cases/$CASE_ID/classify/$JOB_ID until its status is COMPLETED, FAILED or CANCELLED
# docs:end
