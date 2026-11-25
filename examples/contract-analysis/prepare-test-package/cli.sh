#!/usr/bin/env bash
set -euo pipefail

# docs:start
CASE_ID=42  # id of the case that holds the playbook documents
ANCHOR_JOB_GUID="<completed-contract-job>"  # jobGuid of a completed contract job

docudevs knowledge-base contract-analysis package-prepare "$CASE_ID" \
  --anchor-job-guid "$ANCHOR_JOB_GUID" \
  --idempotency-key package-1 \
  --as-of-date 2026-09-22
# docs:end
