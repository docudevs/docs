#!/usr/bin/env bash
set -euo pipefail

# docs:start
METADATA=$(curl -s -S -X POST "https://api.docudevs.ai/document/acroform-metadata" \
  -H "Authorization: $API_KEY" \
  -F "document=@fillable-form.pdf")

echo "$METADATA" | jq -r '"Direct fields: \(.fields | length)"'
echo "$METADATA" | jq '.fields[:3]'
# docs:end
