#!/usr/bin/env bash
set -euo pipefail

# docs:start
CASE_ID=42  # id of a records case
JOB_ID=9  # jobId returned when the job was started

curl -s -S -f -X POST "https://api.docudevs.ai/cases/$CASE_ID/classify/$JOB_ID/cancel" \
  -H "Authorization: $API_KEY" | jq -r '.status'
# docs:end
