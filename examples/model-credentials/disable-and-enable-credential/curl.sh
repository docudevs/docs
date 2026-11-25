#!/usr/bin/env bash
set -euo pipefail

# docs:start
# CREDENTIAL_ID is the "id" returned when the credential was created or listed.
CREDENTIAL_ID=42

# Block every provider that uses the credential
curl -s -S -f -X POST "https://api.docudevs.ai/model-credentials/$CREDENTIAL_ID/disable" \
  -H "Authorization: $API_KEY"

# Allow them again
curl -s -S -f -X POST "https://api.docudevs.ai/model-credentials/$CREDENTIAL_ID/enable" \
  -H "Authorization: $API_KEY"
# docs:end
