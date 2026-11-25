#!/usr/bin/env bash
set -euo pipefail

# docs:start
CASE_ID=42  # id of the case that holds the playbook documents

ANCHOR_JOB_GUID="<completed-contract-job>"  # jobGuid of a completed contract job
SUITE_REVISION=3  # revision returned by the test-suite read

docudevs --token "$DOCUDEVS_API_KEY" knowledge-base contract-analysis trial-estimate "$CASE_ID" \
  --anchor-job-guid "$ANCHOR_JOB_GUID" \
  --suite-revision "$SUITE_REVISION" \
  --kind scored > estimate.json

docudevs --token "$DOCUDEVS_API_KEY" knowledge-base contract-analysis trial "$CASE_ID" \
  --anchor-job-guid "$ANCHOR_JOB_GUID" \
  --idempotency-key trial-key \
  --suite-revision "$SUITE_REVISION" \
  --kind scored \
  --estimate-fingerprint "$(jq -r '.estimateFingerprint' estimate.json)" \
  --configuration-fingerprint "$(jq -r '.executionFingerprint' estimate.json)"
# docs:end
