#!/usr/bin/env bash
set -euo pipefail

# docs:start
RUN_ID="<run-id>"  # runId returned when the run was created
RESULT_FINGERPRINT="<result-fingerprint>"  # resultFingerprint of that run's result

docudevs knowledge-base contract-analysis review "$RUN_ID" hours \
  --result-fingerprint "$RESULT_FINGERPRINT" \
  --disposition corrected \
  --corrected-verdict flagged \
  --expected-review-revision 0 \
  --idempotency-key review-1 \
  --reason "Confirmed the omission against the source clause"
# docs:end
