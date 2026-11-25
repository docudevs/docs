#!/usr/bin/env bash
set -euo pipefail

# docs:start
# CREDENTIAL_ID is the "id" returned when the credential was created or listed.
CREDENTIAL_ID=42

curl -s -S -f -X PATCH "https://api.docudevs.ai/model-credentials/$CREDENTIAL_ID" \
  -H "Authorization: $API_KEY" \
  -H "Content-Type: application/json" \
  -d '{"description": "Updated description"}'
# docs:end
