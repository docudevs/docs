#!/usr/bin/env bash
set -euo pipefail

# docs:start
JOB_GUID="<completed-contract-job>"  # jobGuid of a completed contract job

docudevs --token "$DOCUDEVS_API_KEY" operations submit "$JOB_GUID" \
  --type contract-analysis \
  --parameter knowledgeBaseId=42

docudevs --token "$DOCUDEVS_API_KEY" operations result "$JOB_GUID" --type contract-analysis
# docs:end
