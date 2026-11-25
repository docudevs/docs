#!/usr/bin/env bash
set -euo pipefail

# docs:start
curl -s -S -f -X GET "https://api.docudevs.ai/template/list" \
  -H "Authorization: $API_KEY" | jq -r '.[] | "Name: \(.name), Created: \(.createdAt)"'
# docs:end
