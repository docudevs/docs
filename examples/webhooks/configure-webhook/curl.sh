#!/usr/bin/env bash
set -euo pipefail

# docs:start
# Set the organization webhook URL
curl -s -S -f -X PUT "https://api.docudevs.ai/settings/webhook" \
  -H "Authorization: $API_KEY" \
  -H "Content-Type: application/json" \
  -d '{"url": "https://example.com/webhooks/docudevs"}'

# Check the current webhook settings
curl -s -S -f "https://api.docudevs.ai/settings/webhook" \
  -H "Authorization: $API_KEY"

# Disable webhooks
curl -s -S -f -X PUT "https://api.docudevs.ai/settings/webhook" \
  -H "Authorization: $API_KEY" \
  -H "Content-Type: application/json" \
  -d '{"url": null}'
# docs:end
