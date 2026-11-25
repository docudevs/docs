#!/usr/bin/env bash
set -euo pipefail

# docs:start
CASE_ID=42  # id of the knowledge base, from GET /knowledge-bases

curl -s -S -f -o /dev/null -w "Upload status: %{http_code}\n" -X POST "https://api.docudevs.ai/cases/$CASE_ID/documents" \
  -H "Authorization: $API_KEY" \
  -F "document=@public-holidays-2024.csv" \
  -F "datasetType=calendar"
# docs:end
