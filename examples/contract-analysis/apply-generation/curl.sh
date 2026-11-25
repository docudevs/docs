#!/usr/bin/env bash
set -euo pipefail

# docs:start
CASE_ID=42  # id of the case that holds the playbook documents
GENERATION_JOB_GUID="<generation-job-guid>"  # jobGuid returned by the generate request

COMPARISON=$(curl -s -S -f "https://api.docudevs.ai/knowledge-bases/$CASE_ID/profiles/contract-analysis/generations/$GENERATION_JOB_GUID/comparison" \
  -H "Authorization: $API_KEY")

jq '{
  generationJobGuid: .generationJobGuid,
  expectedDraftRevision: .currentDraftRevision,
  expectedDraftFingerprint: .currentDraftFingerprint,
  decisions: {"hours": {"action": "accept_proposed"}}
}' <<< "$COMPARISON" > apply.json

curl -s -S -f -X POST "https://api.docudevs.ai/knowledge-bases/$CASE_ID/profiles/contract-analysis/generations/$GENERATION_JOB_GUID/apply" \
  -H "Authorization: $API_KEY" \
  -H "Content-Type: application/json" \
  -d @apply.json | jq .
# docs:end
