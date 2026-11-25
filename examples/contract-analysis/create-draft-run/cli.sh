#!/usr/bin/env bash
set -euo pipefail

# docs:start
ANCHOR_JOB_GUID="<completed-contract-job>"  # jobGuid of a completed contract job
DRAFT_REVISION=4  # draftRevision returned by the last import or draft save

docudevs knowledge-base contract-analysis run-create 42 \
  --anchor-job-guid "$ANCHOR_JOB_GUID" \
  --idempotency-key draft-run-1 \
  --mode draft_trial \
  --draft-revision "$DRAFT_REVISION"
# docs:end
