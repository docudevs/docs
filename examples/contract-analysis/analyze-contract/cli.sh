#!/usr/bin/env bash
set -euo pipefail

# docs:start
JOB_GUID="<completed-contract-job>"  # jobGuid of a completed contract job

docudevs operations submit "$JOB_GUID" \
  --type contract-analysis \
  --parameter knowledgeBaseId=42 \
  --wait
# docs:end
