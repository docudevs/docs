#!/usr/bin/env bash
set -euo pipefail

# docs:start
CASE_ID=42  # id of the case that holds the playbook documents

ATTESTATION_ID="<attestation-id>"  # attestationId returned by the attest request

docudevs --token "$DOCUDEVS_API_KEY" knowledge-base contract-analysis publish "$CASE_ID" \
  --draft-revision 2 \
  --trial-attestation-id "$ATTESTATION_ID"
# docs:end
