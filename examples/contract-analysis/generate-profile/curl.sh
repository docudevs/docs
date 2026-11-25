#!/usr/bin/env bash
set -euo pipefail

# docs:start
CASE_ID=42  # id of the case that holds the playbook documents

curl -s -S -X POST "https://api.docudevs.ai/knowledge-bases/$CASE_ID/profiles/contract-analysis/generate" \
  -H "Authorization: $DOCUDEVS_API_KEY" \
  -H "Content-Type: application/json" \
  -d '{"perspective": {"ourParty": "Acme Corp", "counterparty": "Globex Ltd"}}' | jq .
# docs:end
