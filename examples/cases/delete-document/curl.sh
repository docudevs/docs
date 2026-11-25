#!/usr/bin/env bash
set -euo pipefail

# docs:start
CASE_ID=123  # id returned when the case was created
DOCUMENT_ID="doc-uuid-here"  # documentId from the list-documents response

curl -s -S -f -o /dev/null -X DELETE "https://api.docudevs.ai/cases/$CASE_ID/documents/$DOCUMENT_ID" \
  -H "Authorization: $DOCUDEVS_API_KEY"
echo "Document deleted successfully"
# docs:end
