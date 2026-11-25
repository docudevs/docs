#!/usr/bin/env bash
set -uo pipefail

# docs:start
HTTP_STATUS=$(curl -s -S -o /dev/null -w "%{http_code}" "https://api.docudevs.ai/cases/99999" \
  -H "Authorization: $API_KEY")

if [ "$HTTP_STATUS" = "404" ]; then
  echo "Case not found"
fi
# docs:end
