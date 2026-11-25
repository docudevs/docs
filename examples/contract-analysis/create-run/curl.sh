#!/usr/bin/env bash
set -euo pipefail

# docs:start
curl -s -S -f -X POST "https://api.docudevs.ai/contract-analysis/runs" \
  -H "Authorization: $API_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "anchorJobGuid": "<agreement-job>",
    "knowledgeBaseId": 42,
    "mode": "published",
    "idempotencyKey": "request-1",
    "packageMembers": [
      {"jobGuid": "<agreement-job>", "role": "agreement"},
      {"jobGuid": "<amendment-job>", "role": "amendment"}
    ]
  }' | jq .
# docs:end
