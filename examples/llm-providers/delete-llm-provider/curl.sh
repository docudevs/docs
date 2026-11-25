#!/usr/bin/env bash
set -euo pipefail

# docs:start
# PROVIDER_ID is the "id" returned when the provider was created or listed.
PROVIDER_ID=7

curl -s -S -f -X DELETE "https://api.docudevs.ai/llm/providers/$PROVIDER_ID" \
  -H "Authorization: $API_KEY" \
  -w "HTTP %{http_code}\n"
# docs:end
