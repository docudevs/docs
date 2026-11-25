#!/usr/bin/env bash
set -euo pipefail

# docs:start
CASE_ID=42  # id of a records case
JOB_ID=9  # jobId returned when the job was started
ROW_ID=17  # rowId of a result row

curl -s -S -f -X POST "https://api.docudevs.ai/cases/$CASE_ID/classify/$JOB_ID/reviews" \
  -H "Authorization: $API_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "rowId": '"$ROW_ID"',
    "decision": "correct",
    "code": "4.12.3.1",
    "reason": "Plain cotton sock, no sport use."
  }' | jq -r '"\(.reviewId) \(.decision) \(.labelWrite.importId // "-") \(.labelWrite.status // "-")"'
# docs:end
