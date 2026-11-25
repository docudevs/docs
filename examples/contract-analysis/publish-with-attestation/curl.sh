#!/usr/bin/env bash
set -euo pipefail

# docs:start
CASE_ID=42  # id of the case that holds the playbook documents
DRAFT_REVISION=2  # draftRevision of the draft the trial ran against
ATTESTATION_ID="<attestation-id>"  # attestationId returned by the attest request

curl -s -S -X POST "https://api.docudevs.ai/knowledge-bases/$CASE_ID/profiles/contract-analysis/publish" \
  -H "Authorization: $DOCUDEVS_API_KEY" \
  -H "Content-Type: application/json" \
  -d '{"draftRevision": '"$DRAFT_REVISION"', "acknowledgedWarningCodes": [], "trialAttestationId": "'"$ATTESTATION_ID"'"}' | jq .
# docs:end
