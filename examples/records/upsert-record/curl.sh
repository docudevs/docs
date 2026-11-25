#!/usr/bin/env bash
set -euo pipefail

# docs:start
CASE_ID=42  # id of a records case

curl -s -S -f -X PUT "https://api.docudevs.ai/cases/$CASE_ID/records/6410000123456" \
  -H "Authorization: $API_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "text": {"title": "Soft sock 3-pack", "description": "Cotton, size 40-45"},
    "label": "4.12.3",
    "labelSource": "import",
    "metadata": {"brand": "Foo"}
  }' | jq -r '"Import \(.importId): \(.status)"'
# docs:end
