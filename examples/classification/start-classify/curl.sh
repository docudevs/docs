#!/usr/bin/env bash
set -euo pipefail

# docs:start
CASE_ID=42  # id of a records case with an active category tree

curl -s -S -f -X POST "https://api.docudevs.ai/cases/$CASE_ID/classify" \
  -H "Authorization: $API_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "rows": {
      "source": "inline",
      "items": [
        {
          "key": "6410000999999",
          "text": {"title": "Running socks 3-pack", "description": "Cotton blend"}
        }
      ]
    },
    "config": {"target": "category", "llmLevel": "MINI", "targetDepth": [4, 5]}
  }' | jq -r '"Classify job \(.jobId): \(.status)"'
# docs:end
