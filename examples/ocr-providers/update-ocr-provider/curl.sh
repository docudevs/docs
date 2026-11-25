#!/usr/bin/env bash
set -euo pipefail

# docs:start
# PROVIDER_ID is the "id" returned when the provider was created or listed.
PROVIDER_ID=7

curl -s -S -f -X PATCH "https://api.docudevs.ai/ocr/providers/$PROVIDER_ID" \
  -H "Authorization: $API_KEY" \
  -H "Content-Type: application/json" \
  -d '{"modelId": "prebuilt-invoice"}'
# docs:end
