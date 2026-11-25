#!/usr/bin/env bash
set -euo pipefail

# docs:start
curl -s -S -X GET "https://api.docudevs.ai/configuration/invoice-config" \
  -H "Authorization: $API_KEY" | jq '{prompt, ocr, llm}'
# docs:end
