#!/usr/bin/env bash
set -euo pipefail

# docs:start
# CREDENTIAL_ID is the "id" of a model credential (see Model credentials).
CREDENTIAL_ID=42

curl -s -S -f -X POST "https://api.docudevs.ai/llm/providers" \
  -H "Authorization: $API_KEY" \
  -H "Content-Type: application/json" \
  -d "$(jq -n --argjson credentialId "$CREDENTIAL_ID" '{
    name: "Our Azure OpenAI",
    apiUrl: "https://example.openai.azure.com",
    modelType: "azure-openai",
    deploymentName: "gpt-4o",
    credentialId: $credentialId
  }')"
# docs:end
