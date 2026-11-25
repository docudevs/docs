#!/usr/bin/env bash
set -euo pipefail

# docs:start
CASE_ID=42  # id of the case that holds the playbook documents
DOCUMENT_ID="<document-id>"  # id of a completed document in the case

curl -s -S -f -o /dev/null -w "Update status: %{http_code}\n" -X PATCH "https://api.docudevs.ai/cases/$CASE_ID/documents/$DOCUMENT_ID" \
  -H "Authorization: $API_KEY" \
  -H "Content-Type: application/json" \
  -d '{"datasetType": "test_contract", "testContractRetention": "kept"}'
# docs:end
