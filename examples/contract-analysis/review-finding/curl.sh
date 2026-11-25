#!/usr/bin/env bash
set -euo pipefail

# docs:start
RUN_ID="<run-id>"  # runId returned when the run was created
RESULT_FINGERPRINT="<result-fingerprint>"  # resultFingerprint of that run's result

curl -s -S -X PUT "https://api.docudevs.ai/contract-analysis/runs/$RUN_ID/reviews/hours" \
  -H "Authorization: $DOCUDEVS_API_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "resultFingerprint": "'"$RESULT_FINGERPRINT"'",
    "disposition": "corrected",
    "correctedVerdict": "flagged",
    "expectedReviewRevision": 0,
    "idempotencyKey": "review-1",
    "reason": "Confirmed the omission against the source clause"
  }' | jq .
# docs:end
