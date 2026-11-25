#!/usr/bin/env bash
set -euo pipefail

# docs:start
# CREDENTIAL_ID is the "id" of a shared model credential (see Model credentials).
CREDENTIAL_ID=42

curl -s -S -f -X POST "https://api.docudevs.ai/embeddings/providers" \
  -H "Authorization: $API_KEY" \
  -H "Content-Type: application/json" \
  -d "$(jq -n --argjson credentialId "$CREDENTIAL_ID" '{
    name: "Our OpenAI embeddings",
    providerType: "openai",
    apiUrl: "",
    deploymentName: "text-embedding-3-small",
    dimensions: 1536,
    similarityMetric: "cosine",
    requestOptions: {},
    credentialId: $credentialId,
    setAsDefault: false
  }')"
# docs:end
