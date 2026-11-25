#!/usr/bin/env bash
set -euo pipefail

# docs:start
# DOC_INTELLIGENCE_API_KEY holds the key of your Azure Document Intelligence resource.
curl -s -S -f -X POST "https://api.docudevs.ai/ocr/providers" \
  -H "Authorization: $API_KEY" \
  -H "Content-Type: application/json" \
  -d "$(jq -n --arg apiKey "$DOC_INTELLIGENCE_API_KEY" '{
    name: "Our Doc Intelligence",
    endpoint: "https://example.cognitiveservices.azure.com",
    apiKey: $apiKey,
    modelId: "prebuilt-document"
  }')"
# docs:end
