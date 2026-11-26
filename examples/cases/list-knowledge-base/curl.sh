#!/usr/bin/env bash
set -euo pipefail

# docs:start
curl -s -S -f "https://api.docudevs.ai/knowledge-bases" \
  -H "Authorization: $API_KEY" | jq -r '(. // [])[] | "Knowledge base \(.id): \(.name)"'
# docs:end
