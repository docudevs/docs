#!/usr/bin/env bash
set -euo pipefail

# docs:start
CASE_ID=42  # id of a records case

curl -s -S -f "https://api.docudevs.ai/cases/$CASE_ID/records/6410000123456" \
  -H "Authorization: $API_KEY" | jq -r '.label, (.labelPath | join(" > ")), .labelSource, .chunk'
# docs:end
