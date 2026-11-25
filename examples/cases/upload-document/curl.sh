#!/usr/bin/env bash
set -euo pipefail

# docs:start
CASE_ID=123  # id returned when the case was created

curl -s -S -f -o /dev/null -w "Upload status: %{http_code}\n" -X POST "https://api.docudevs.ai/cases/$CASE_ID/documents" \
  -H "Authorization: $API_KEY" \
  -F "document=@invoice_001.pdf"
# docs:end
