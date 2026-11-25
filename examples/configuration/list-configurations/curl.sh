#!/usr/bin/env bash
set -euo pipefail

# docs:start
curl -s -S -X GET "https://api.docudevs.ai/configuration" \
  -H "Authorization: $API_KEY" | jq -r '.[] | "Name: \(.name), Created: \(.createdAt)"'
# docs:end
