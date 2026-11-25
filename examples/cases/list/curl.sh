#!/usr/bin/env bash
set -euo pipefail

# docs:start
curl -s -S -X GET "https://api.docudevs.ai/cases" \
  -H "Authorization: $API_KEY" | jq -r '.[] | "Case \(.id): \(.name)"'
# docs:end
