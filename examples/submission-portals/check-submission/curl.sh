#!/usr/bin/env bash
set -euo pipefail

# docs:start
# RECEIPT_CODE is the "receiptCode" returned by the submission.
RECEIPT_CODE=AB12CD34

# Check the status: CREATED, UPLOADING, PROCESSING, COMPLETED, FAILED or PARTIAL
curl -s -S -f "https://api.docudevs.ai/submission-records/$RECEIPT_CODE" \
  -H "Authorization: $API_KEY"

# Download the result once the status is COMPLETED (409 while it is not ready)
curl -s -S -f "https://api.docudevs.ai/submission-records/$RECEIPT_CODE/download" \
  -H "Authorization: $API_KEY" \
  -o result.xlsx
# docs:end
