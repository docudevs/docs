#!/usr/bin/env bash
set -euo pipefail

# docs:start
CASE_ID=42  # id of a records case

curl -s -S -f -X POST "https://api.docudevs.ai/cases/$CASE_ID/search" \
  -H "Authorization: $API_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "kind": "records",
    "query": "Soft sports sock",
    "topK": 10,
    "filters": {"subtree": "4.12", "attributes": {"brand": "Foo"}}
  }' | jq -r '.results[]? | "\(.key) \(.label) \(.score)"'
# docs:end
