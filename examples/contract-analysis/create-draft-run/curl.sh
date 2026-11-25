#!/usr/bin/env bash
set -euo pipefail

# docs:start
ANCHOR_JOB_GUID="<completed-contract-job>"  # jobGuid of a completed contract job
DRAFT_REVISION=4  # draftRevision returned by the last import or draft save

curl -s -S -f -X POST "https://api.docudevs.ai/contract-analysis/runs" \
  -H "Authorization: $API_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "anchorJobGuid": "'"$ANCHOR_JOB_GUID"'",
    "knowledgeBaseId": 42,
    "mode": "draft_trial",
    "draftRevision": '"$DRAFT_REVISION"',
    "idempotencyKey": "draft-run-1"
  }' | jq -r '"Run ID: \(.runId)"'
# docs:end
