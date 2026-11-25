#!/usr/bin/env bash
set -euo pipefail

# docs:start
curl -s -S -f -o /dev/null -X DELETE "https://api.docudevs.ai/configuration/invoice-config" \
  -H "Authorization: $API_KEY"
echo "Deleted configuration invoice-config"
# docs:end
