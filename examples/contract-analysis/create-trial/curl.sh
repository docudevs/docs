#!/usr/bin/env bash
set -euo pipefail

# docs:start
CASE_ID=42  # id of the case that holds the playbook documents
ANCHOR_JOB_GUID="<completed-contract-job>"  # jobGuid of a completed contract job
SUITE_REVISION=3  # revision returned by the test-suite read

ESTIMATE=$(curl -s -S -f -X POST "https://api.docudevs.ai/knowledge-bases/$CASE_ID/profiles/contract-analysis/trials/estimate" \
  -H "Authorization: $API_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "anchorJobGuid": "'"$ANCHOR_JOB_GUID"'",
    "trialKind": "scored",
    "suiteRevision": '"$SUITE_REVISION"'
  }')

jq '{
  anchorJobGuid: "'"$ANCHOR_JOB_GUID"'",
  idempotencyKey: "trial-key",
  trialKind: "scored",
  suiteRevision: .suiteRevision,
  estimateFingerprint: .estimateFingerprint,
  configurationFingerprint: .executionFingerprint
}' <<< "$ESTIMATE" > trial.json

curl -s -S -f -X POST "https://api.docudevs.ai/knowledge-bases/$CASE_ID/profiles/contract-analysis/trials" \
  -H "Authorization: $API_KEY" \
  -H "Content-Type: application/json" \
  -d @trial.json | jq .
# docs:end
