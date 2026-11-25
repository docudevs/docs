#!/usr/bin/env bash
set -euo pipefail

# docs:start
CASE_ID=42  # id of the case that holds the playbook documents

curl -s -S -f "https://api.docudevs.ai/knowledge-bases/$CASE_ID/profiles/contract-analysis/draft/revisions" \
  -H "Authorization: $API_KEY" | jq -r '.revisions[] | "\(.draftRevision) \(.actorId // "-") \(.reason // "-")"'

curl -s -S -f "https://api.docudevs.ai/knowledge-bases/$CASE_ID/profiles/contract-analysis/draft/revisions/2" \
  -H "Authorization: $API_KEY" | jq -r '"Revision \(.draftRevision): \(.reason // "-")"'
# docs:end
