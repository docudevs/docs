#!/usr/bin/env bash
set -euo pipefail

# docs:start
CASE_ID=42  # id of the case that holds the playbook documents

AUTHORITY=$(curl -s -S -f "https://api.docudevs.ai/knowledge-bases/$CASE_ID/profiles/contract-analysis/source-authority" \
  -H "Authorization: $API_KEY")

# Every guideline document of the case must be listed, with the fingerprint read above.
# This example confirms each document as policy; set the role of each document.
jq -n \
  --argjson authority "$AUTHORITY" \
  '{
    observedRevision: $authority.revision,
    decisionType: "source_authority",
    content: {
      documents: [($authority.sources // [])[]
        | {id: .documentId, contentFingerprint: .contentFingerprint, sourceRole: "policy"}]
    },
    reason: "Reviewer confirmed the governing policy documents"
  }' \
  | curl -s -S -f -X POST "https://api.docudevs.ai/knowledge-bases/$CASE_ID/profiles/contract-analysis/source-authority/decisions" \
      -H "Authorization: $API_KEY" \
      -H "Content-Type: application/json" \
      -d @- | jq .
# docs:end
