#!/usr/bin/env bash
set -euo pipefail

# docs:start
CASE_ID=42  # id of a records case
VERSION_ID=7  # treeVersionId returned when the tree was uploaded

curl -s -S -f "https://api.docudevs.ai/cases/$CASE_ID/trees/$VERSION_ID/nodes/4.12.3" \
  -H "Authorization: $API_KEY" | jq '{code, name, path, children: [(.children // [])[].code], siblings: [(.siblings // [])[].code]}'
# docs:end
