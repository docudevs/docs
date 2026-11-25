#!/usr/bin/env bash
set -euo pipefail

# docs:start
CASE_ID=42  # id of a records case

curl -s -S -f -X POST "https://api.docudevs.ai/cases/$CASE_ID/records:batch" \
  -H "Authorization: $API_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "upserts": [
      {
        "key": "6410000123457",
        "text": {"title": "Wool sock", "description": "Merino, size 36-40"},
        "label": "4.12.3",
        "metadata": {"brand": "Foo"}
      }
    ],
    "deletes": ["6410000123456"]
  }' | jq -r '"Import \(.importId): \(.status)"'
# docs:end
