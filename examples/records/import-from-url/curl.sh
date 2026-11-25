#!/usr/bin/env bash
set -euo pipefail

# docs:start
CASE_ID=42  # id of a records case with a configured dataset

# The worker fetches the file, so the URL must be https and publicly reachable.
# The import is queued when it is created; there is no commit step.
curl -s -S -f -X POST "https://api.docudevs.ai/cases/$CASE_ID/records/imports" \
  -H "Authorization: $API_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "mode": "upsert",
    "format": "csv",
    "gzip": false,
    "sourceUrl": "https://example.com/exports/products.csv"
  }' | jq '{importId, status}'
# Read GET /cases/$CASE_ID/records/imports/<importId> until its status is COMPLETED or FAILED
# docs:end
