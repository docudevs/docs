#!/usr/bin/env bash
set -euo pipefail

# docs:start
# CREDENTIAL_ID is the "id" returned when the credential was created or listed.
# NEW_PROVIDER_API_KEY holds the replacement secret.
CREDENTIAL_ID=42

curl -s -S -f -X POST "https://api.docudevs.ai/model-credentials/$CREDENTIAL_ID/rotate" \
  -H "Authorization: $API_KEY" \
  -H "Content-Type: application/json" \
  -d "$(jq -n --arg secret "$NEW_PROVIDER_API_KEY" '{secret: $secret}')"
# docs:end
