#!/usr/bin/env bash
set -euo pipefail

# docs:start
CASE_ID=42  # id of a case that holds documents and no records dataset

curl -s -S -f -X POST "https://api.docudevs.ai/cases/$CASE_ID/search" \
  -H "Authorization: $API_KEY" \
  -H "Content-Type: application/json" \
  -d '{"kind": "documents", "query": "termination notice period", "topK": 5}' \
  | jq -r '.results[]? | "\(.filename) page \(.pageNumber)"'
# docs:end
