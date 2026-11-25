#!/usr/bin/env bash
set -euo pipefail

# docs:start
# CREDENTIAL_ID is the "id" returned when the credential was created or listed.
CREDENTIAL_ID=42

curl -s -S -f "https://api.docudevs.ai/model-credentials/$CREDENTIAL_ID" \
  -H "Authorization: $API_KEY"
# docs:end
