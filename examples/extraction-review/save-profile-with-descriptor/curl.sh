#!/usr/bin/env bash
set -euo pipefail

# docs:start
# descriptor.json is written by the previous example.
# The configuration invoice-v1 must already exist.
PREVIEW=$(jq -n --rawfile descriptor descriptor.json '{clientPipelineJson: $descriptor}' |
  curl -s -S -f -X POST "https://api.docudevs.ai/configuration/invoice-v1/extraction-review-profile/preview" \
    -H "Authorization: $API_KEY" \
    -H "Content-Type: application/json" \
    --data-binary @-)

echo "$PREVIEW" | jq '{
    expectedRevision: .revision,
    clientPipelineJson: .clientPipelineJson,
    enabled: true
  }' |
  curl -s -S -f -X PUT "https://api.docudevs.ai/configuration/invoice-v1/extraction-review-profile" \
    -H "Authorization: $API_KEY" \
    -H "Content-Type: application/json" \
    --data-binary @- | jq '{revision, enabled, producerFingerprint}'
# docs:end
