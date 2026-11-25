#!/usr/bin/env bash
set -euo pipefail

# docs:start
curl -s -S -f -X POST "https://api.docudevs.ai/template/invoice" \
  -H "Authorization: $API_KEY" \
  -F "document=@invoice_template.pdf" | jq .
# docs:end
