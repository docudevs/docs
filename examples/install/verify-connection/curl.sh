#!/usr/bin/env bash
set -euo pipefail

# docs:start
# List your saved configurations to confirm the API key works
STATUS=$(curl -s -S -o /dev/null -w "%{http_code}" "https://api.docudevs.ai/configuration" \
  -H "Authorization: $API_KEY")

if [ "$STATUS" = "200" ]; then
  echo "Connection successful! Status code: $STATUS"
else
  echo "Connection failed! Status code: $STATUS" >&2
  exit 1
fi
# docs:end
