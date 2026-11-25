#!/usr/bin/env bash
set -euo pipefail

# docs:start
# ENTRA_CLIENT_SECRET holds the client secret value copied from Entra.
curl -s -S -f -X POST "https://api.docudevs.ai/model-credentials" \
  -H "Authorization: $API_KEY" \
  -H "Content-Type: application/json" \
  -d "$(jq -n --arg secret "$ENTRA_CLIENT_SECRET" '{
    name: "Enterprise AI gateway",
    description: "Shared by chat and embeddings",
    authType: "ENTRA_CLIENT_SECRET",
    tenantId: "00000000-0000-0000-0000-000000000000",
    clientId: "11111111-1111-1111-1111-111111111111",
    secret: $secret,
    scope: "api://22222222-2222-2222-2222-222222222222/.default",
    cloud: "PUBLIC",
    secretExpiresAt: "2027-08-25T00:00:00Z"
  }')"
# docs:end
