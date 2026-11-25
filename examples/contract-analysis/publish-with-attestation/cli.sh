#!/usr/bin/env bash
set -euo pipefail

# docs:start
ATTESTATION_ID="<attestation-id>"  # attestationId returned by the attest request

docudevs --token "$DOCUDEVS_API_KEY" knowledge-base contract-analysis publish 42 \
  --draft-revision 2 \
  --trial-attestation-id "$ATTESTATION_ID"
# docs:end
