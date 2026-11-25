#!/usr/bin/env bash
set -euo pipefail

# docs:start
# PROVIDER_ID is the "id" returned when the provider was created or listed.
PROVIDER_ID=7

# List the current bindings of DEFAULT, NANO, MINI and HIGH
curl -s -S -f "https://api.docudevs.ai/llm/keys" \
  -H "Authorization: $API_KEY"

# Bind the HIGH level to the provider; send {"providerId": null} to clear a binding
curl -s -S -f -X PUT "https://api.docudevs.ai/llm/keys/HIGH" \
  -H "Authorization: $API_KEY" \
  -H "Content-Type: application/json" \
  -d "$(jq -n --argjson providerId "$PROVIDER_ID" '{providerId: $providerId}')"
# docs:end
