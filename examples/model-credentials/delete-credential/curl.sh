#!/usr/bin/env bash
set -euo pipefail

# docs:start
# CREDENTIAL_ID is the "id" returned when the credential was created or listed.
CREDENTIAL_ID=42

# 204 when deleted, 409 while a provider still references the credential
curl -s -S -X DELETE "https://api.docudevs.ai/model-credentials/$CREDENTIAL_ID" \
  -H "Authorization: $API_KEY" \
  -w "HTTP %{http_code}\n"
# docs:end
