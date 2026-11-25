#!/usr/bin/env bash
set -euo pipefail

# docs:start
curl -s -S -f -X GET "https://api.docudevs.ai/configuration/invoice-config" \
  -H "Authorization: $API_KEY" | jq '{prompt, ocr, llm}'
# docs:end
