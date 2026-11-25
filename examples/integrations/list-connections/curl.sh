#!/usr/bin/env bash
set -euo pipefail

# docs:start
curl -s -S -f "https://api.docudevs.ai/integration/connections" \
  -H "Authorization: $API_KEY"
# docs:end
