#!/usr/bin/env bash
set -euo pipefail

# docs:start
CASE_ID=42  # id of a records case
JOB_ID=9  # jobId returned when the job was started

# One page (limit 1 to 1,000, default 100); pass .continuation back as
# --data-urlencode "continuation=<token>" to read the next page. The Python
# client follows the tokens for you.
curl -s -S -f -G "https://api.docudevs.ai/cases/$CASE_ID/classify/$JOB_ID/results" \
  -H "Authorization: $API_KEY" \
  --data-urlencode "reviewRequired=true" | jq -r '.rows[] | "\(.rowId) \(.key // "-") \(.chosen.code // "-") \(.review.reasons // [] | join(","))"'
# docs:end
