#!/usr/bin/env bash
set -uo pipefail

# docs:start
HTTP_STATUS=$(curl -s -S -o /dev/null -w "%{http_code}" "https://api.docudevs.ai/cases" \
  -H "Authorization: invalid-key")

if [ "$HTTP_STATUS" = "401" ]; then
  echo "Authentication failed (401)"
fi
# docs:end
