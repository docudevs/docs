#!/usr/bin/env bash
set -euo pipefail

# docs:start
curl -s -S -f -o /dev/null -X DELETE "https://api.docudevs.ai/template/invoice" \
  -H "Authorization: $API_KEY"
echo "Deleted template invoice"
# docs:end
