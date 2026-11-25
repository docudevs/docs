#!/usr/bin/env bash
set -euo pipefail

# docs:start
CASE_ID=42  # id of the case that holds the playbook documents
ANCHOR_JOB_GUID="<completed-contract-job>"  # jobGuid of a completed contract job

curl -s -S -f -X POST "https://api.docudevs.ai/knowledge-bases/$CASE_ID/profiles/contract-analysis/test-packages" \
  -H "Authorization: $API_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "anchorJobGuid": "'"$ANCHOR_JOB_GUID"'",
    "idempotencyKey": "package-1",
    "packageMembers": [{"jobGuid": "'"$ANCHOR_JOB_GUID"'", "role": "agreement"}],
    "context": {"asOfDate": "2026-09-22"}
  }' | jq -r '"Package \(.packageId), fingerprint \(.fingerprint)"'
# docs:end
