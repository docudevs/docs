#!/usr/bin/env bash
set -euo pipefail

# docs:start
CASE_ID=123  # id returned when the case was created

curl -s -S -X GET "https://api.docudevs.ai/cases/$CASE_ID/documents?page=0&size=20" \
  -H "Authorization: $DOCUDEVS_API_KEY" | jq .
# docs:end
