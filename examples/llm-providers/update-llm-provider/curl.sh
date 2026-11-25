#!/usr/bin/env bash
set -euo pipefail

# docs:start
# PROVIDER_ID is the "id" returned when the provider was created or listed.
PROVIDER_ID=7

curl -s -S -f -X PATCH "https://api.docudevs.ai/llm/providers/$PROVIDER_ID" \
  -H "Authorization: $API_KEY" \
  -H "Content-Type: application/json" \
  -d '{"deploymentName": "gpt-4o-mini"}'
# docs:end
