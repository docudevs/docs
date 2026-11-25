#!/usr/bin/env bash
set -euo pipefail

# docs:start
CASE_ID=42  # id of the case that holds the playbook documents

curl -s -S "https://api.docudevs.ai/knowledge-bases/$CASE_ID/profiles/contract-analysis?view=draft" \
  -H "Authorization: $API_KEY" > draft.json

jq '{draftRevision: .draftRevision, profile: .profile, reason: "Reviewed clause wording"}' draft.json > update.json
# edit the profile in update.json, then send it back at the revision you read

curl -s -S -X PUT "https://api.docudevs.ai/knowledge-bases/$CASE_ID/profiles/contract-analysis/draft" \
  -H "Authorization: $API_KEY" \
  -H "Content-Type: application/json" \
  -d @update.json | jq .
# docs:end
