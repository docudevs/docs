#!/usr/bin/env bash
set -euo pipefail

# docs:start
JOB_GUID="<job-guid>"  # GUID of a completed OCR-only job

# Print the HTTP status of each result route instead of failing on it
for ROUTE in "" "/json" "/csv" "/excel" "/schema"; do
  CODE=$(curl -s -S -o /dev/null -w "%{http_code}" "https://api.docudevs.ai/job/result/$JOB_GUID$ROUTE" \
    -H "Authorization: $API_KEY")
  echo "/job/result/{guid}$ROUTE -> $CODE"
done
# docs:end
