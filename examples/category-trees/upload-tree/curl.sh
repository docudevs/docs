#!/usr/bin/env bash
set -euo pipefail

# docs:start
CASE_ID=42  # id of a records case

STATUS=$(curl -s -S -o tree-response.json -w '%{http_code}' -X POST "https://api.docudevs.ai/cases/$CASE_ID/trees" \
  -H "Authorization: $API_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Catalogue 2026-10",
    "nodes": [
      {"code": "4", "parent": null, "name": "Clothing"},
      {"code": "4.12", "parent": "4", "name": "Socks and tights"},
      {"code": "4.12.3", "parent": "4.12", "name": "Socks", "description": "Socks worn inside shoes"},
      {"code": "9.90.1", "parent": "9.90", "name": "OLD Socks", "status": "retired", "successor": "4.12.3"}
    ]
  }')

if [ "$STATUS" != "201" ]; then
  # A rejected tree stores nothing; the body lists every problem
  jq -r '.problems[]? | "\(.type) \(.code // "-") \(.message)"' tree-response.json >&2
  exit 1
fi
jq '{treeVersionId, status, nodes, levels}' tree-response.json
# docs:end
