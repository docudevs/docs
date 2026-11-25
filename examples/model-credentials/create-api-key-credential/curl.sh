#!/usr/bin/env bash
set -euo pipefail

# docs:start
# PROVIDER_API_KEY holds the API key issued by your model provider.
curl -s -S -f -X POST "https://api.docudevs.ai/model-credentials" \
  -H "Authorization: $API_KEY" \
  -H "Content-Type: application/json" \
  -d "$(jq -n --arg secret "$PROVIDER_API_KEY" '{
    name: "Provider embeddings",
    authType: "API_KEY",
    secret: $secret
  }')"
# docs:end
