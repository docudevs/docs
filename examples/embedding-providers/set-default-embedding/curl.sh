#!/usr/bin/env bash
set -euo pipefail

# docs:start
# REVISION_ID is the revision "id" under provider.latestRevision when the provider was created.
REVISION_ID=12

curl -s -S -f -X PUT "https://api.docudevs.ai/embeddings/default" \
  -H "Authorization: $API_KEY" \
  -H "Content-Type: application/json" \
  -d "$(jq -n --argjson revisionId "$REVISION_ID" '{revisionId: $revisionId}')"

# Counts of cases on the default revision, on older revisions and on the original index
curl -s -S -f "https://api.docudevs.ai/embeddings/overview" \
  -H "Authorization: $API_KEY" | jq .defaultBinding
# docs:end
