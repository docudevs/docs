#!/usr/bin/env bash
set -euo pipefail

# docs:start
# PROVIDER_ID is the "id" returned when the provider was created or listed.
PROVIDER_ID=7

# 202 Accepted: the probe runs in the background
curl -s -S -f -X POST "https://api.docudevs.ai/llm/providers/$PROVIDER_ID/verify" \
  -H "Authorization: $API_KEY" \
  -w "HTTP %{http_code}\n"
# docs:end
