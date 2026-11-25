#!/usr/bin/env bash
set -euo pipefail

# docs:start
CASE_ID=42  # id of a records case

curl -s -S -f "https://api.docudevs.ai/cases/$CASE_ID/records/stats" \
  -H "Authorization: $API_KEY" | jq -r '"\(.records) records, \(.unlabelled) without a label", ((.labels // {}) | to_entries[] | "\(.key) \(.value)")'
# docs:end
