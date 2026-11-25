#!/usr/bin/env bash
set -euo pipefail

# docs:start
CASE_ID=42  # id of the case that holds the playbook documents

RUN_ID="<run-id>"  # runId returned when the run was created
RESULT_FINGERPRINT="<result-fingerprint>"  # resultFingerprint of that run's result

docudevs knowledge-base contract-analysis suite-get "$CASE_ID" > suite.json

docudevs knowledge-base contract-analysis propose-test-case "$CASE_ID" \
  --run-id "$RUN_ID" \
  --result-fingerprint "$RESULT_FINGERPRINT" \
  --clause-id hours \
  --name "Hours-clause omission" \
  --expected-verdict flagged \
  --reason "Reviewer correction" \
  --expected-suite-revision "$(jq '.revision' suite.json)" \
  --idempotency-key proposal-1
# docs:end
