#!/usr/bin/env bash
set -euo pipefail

# docs:start
CASE_ID=123  # id returned when the case was created

curl -s -S -X GET "https://api.docudevs.ai/cases/$CASE_ID/summaries" \
  -H "Authorization: $DOCUDEVS_API_KEY" | jq -r ".[] | \"\(.filename): \(.summary.document_type)\""
# docs:end
