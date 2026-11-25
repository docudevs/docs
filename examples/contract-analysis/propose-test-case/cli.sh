#!/usr/bin/env bash
set -euo pipefail

# docs:start
RUN_ID="<run-id>"  # runId returned when the run was created
RESULT_FINGERPRINT="<result-fingerprint>"  # resultFingerprint of that run's result

docudevs --token "$DOCUDEVS_API_KEY" knowledge-base contract-analysis suite-get 42 > suite.json

docudevs --token "$DOCUDEVS_API_KEY" knowledge-base contract-analysis propose-test-case 42 \
  --run-id "$RUN_ID" \
  --result-fingerprint "$RESULT_FINGERPRINT" \
  --clause-id hours \
  --name "Hours-clause omission" \
  --expected-verdict flagged \
  --reason "Reviewer correction" \
  --expected-suite-revision "$(jq '.revision' suite.json)" \
  --idempotency-key proposal-1
# docs:end
