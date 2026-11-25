#!/usr/bin/env bash
set -euo pipefail

# docs:start
CASE_ID=42  # id of the case that holds the playbook documents
DRAFT_REVISION=2  # draftRevision returned by the last draft update
ACKNOWLEDGED_WARNING_CODES='[]'  # List here the warning codes from the readiness or publish response that you accept.

curl -s -S -f -X POST "https://api.docudevs.ai/knowledge-bases/$CASE_ID/profiles/contract-analysis/publish" \
  -H "Authorization: $API_KEY" \
  -H "Content-Type: application/json" \
  -d '{"draftRevision": '"$DRAFT_REVISION"', "acknowledgedWarningCodes": '"$ACKNOWLEDGED_WARNING_CODES"'}' | jq .
# docs:end
