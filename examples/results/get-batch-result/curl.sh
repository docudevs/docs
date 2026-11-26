#!/usr/bin/env bash
set -euo pipefail

# docs:start
BATCH_GUID="<batch-guid>"  # GUID of a batch whose processing has finished

# Batch overview: state and one entry per document
curl -s -S -f "https://api.docudevs.ai/job/result/$BATCH_GUID" \
  -H "Authorization: $API_KEY" \
  | jq -r '"state: \(.state), completed: \(.completed // 0), failed: \(.failed // 0)", ((.documents // [])[] | "document \(.index): \(.status)")'

# Only the results, in upload order; a document without a result is null
curl -s -S -f "https://api.docudevs.ai/job/result/$BATCH_GUID/json" \
  -H "Authorization: $API_KEY" | jq .
# docs:end
