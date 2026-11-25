#!/usr/bin/env bash
set -euo pipefail

# docs:start
CASE_ID=42  # id of a records case

curl -s -S -f "https://api.docudevs.ai/cases/$CASE_ID/trees" \
  -H "Authorization: $API_KEY" | jq -r '.items[] | "\(.treeVersionId) \(.status) \(.nodes) \(.levels)"'
# docs:end
