#!/usr/bin/env bash
set -euo pipefail

# docs:start
CASE_ID=123  # id returned when the case was created
DOCUMENT_ID="doc-uuid-here"  # documentId from the list-documents response

curl -s -S -X GET "https://api.docudevs.ai/cases/$CASE_ID/documents/$DOCUMENT_ID/summary" \
  -H "Authorization: $DOCUDEVS_API_KEY" | jq .
# docs:end
