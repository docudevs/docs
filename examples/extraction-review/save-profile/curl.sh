#!/usr/bin/env bash
set -euo pipefail

# docs:start
# Preview and save need a signed-in administrator: an organization API key is rejected.
SCHEMA='{"type":"object","properties":{"total":{"type":"number"}}}'

PREVIEW=$(jq -n --arg schema "$SCHEMA" '{schemaJson: $schema, overlay: null}' |
  curl -s -S -f -X POST "https://api.docudevs.ai/configuration/invoice-v1/extraction-review-profile/preview" \
    -H "Authorization: $API_KEY" \
    -H "Content-Type: application/json" \
    --data-binary @-)

echo "$PREVIEW" | jq '{
    expectedRevision: .revision,
    enabled: true,
    schemaJson: .schemaJson,
    overlay: null
  }' |
  curl -s -S -f -X PUT "https://api.docudevs.ai/configuration/invoice-v1/extraction-review-profile" \
    -H "Authorization: $API_KEY" \
    -H "Content-Type: application/json" \
    --data-binary @- | jq '{revision, enabled}'
# docs:end
