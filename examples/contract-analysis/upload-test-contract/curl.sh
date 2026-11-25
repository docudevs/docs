#!/usr/bin/env bash
set -euo pipefail

# docs:start
CASE_ID=42  # id of the case that holds the playbook documents

curl -s -S -f -o /dev/null -w "Upload status: %{http_code}\n" -X POST "https://api.docudevs.ai/cases/$CASE_ID/documents" \
  -H "Authorization: $API_KEY" \
  -F "document=@supplier-agreement.pdf" \
  -F "datasetType=test_contract" \
  -F "testContractRetention=kept"
# docs:end
