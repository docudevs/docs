#!/usr/bin/env bash
set -euo pipefail

# docs:start
CASE_ID=42  # id of the case that holds the playbook documents

OBSERVED_REVISION=$(curl -s -S "https://api.docudevs.ai/knowledge-bases/$CASE_ID/profiles/contract-analysis/source-authority" \
  -H "Authorization: $DOCUDEVS_API_KEY" | jq '.revision')

# "policy-doc" in the decision below must be the id of a real source document in the case
curl -s -S -X POST "https://api.docudevs.ai/knowledge-bases/$CASE_ID/profiles/contract-analysis/source-authority/decisions" \
  -H "Authorization: $DOCUDEVS_API_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "observedRevision": '"$OBSERVED_REVISION"',
    "decisionType": "source_authority",
    "content": {
      "documents": [
        {"id": "policy-doc", "sourceRole": "policy", "excluded": false}
      ]
    },
    "reason": "Reviewer confirmed the governing policy document",
    "tombstone": false
  }' | jq .
# docs:end
