#!/usr/bin/env bash
set -euo pipefail

# docs:start
CASE_ID=42  # id of an organization-wide case, created with an API key

curl -s -S -f -X POST "https://api.docudevs.ai/knowledge-bases" \
  -H "Authorization: $API_KEY" \
  -H "Content-Type: application/json" \
  -d "{\"caseId\": $CASE_ID}" | jq '{id, name, isKnowledgeBase}'
# docs:end
