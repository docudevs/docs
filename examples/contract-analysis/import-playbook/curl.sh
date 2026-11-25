#!/usr/bin/env bash
set -euo pipefail

# docs:start
CASE_ID=42  # id of the case that holds the playbook documents

REVISION=$(curl -s -S -f "https://api.docudevs.ai/knowledge-bases/$CASE_ID/profiles/contract-analysis?view=draft" \
  -H "Authorization: $API_KEY" | jq -r '.draftRevision')  # a new playbook is at revision 0

# playbook.json holds a playbook in the playbook format
jq --argjson revision "$REVISION" '{draftRevision: $revision, playbook: .}' playbook.json > import.json

STATUS=$(curl -s -S -o import-response.json -w '%{http_code}' -X POST "https://api.docudevs.ai/knowledge-bases/$CASE_ID/profiles/contract-analysis/import" \
  -H "Authorization: $API_KEY" \
  -H "Content-Type: application/json" \
  -d @import.json)

if [ "$STATUS" = "422" ]; then
  # A playbook with mistakes is refused with every problem and its JSON path
  jq -r '(.validation.errors // [])[] | "\(.path) \(.code) \(.message)"' import-response.json
elif [ "$STATUS" = "200" ]; then
  jq -r '"Draft revision: \(.draftRevision)"' import-response.json
else
  echo "Import failed with status $STATUS" >&2
  exit 1
fi
# docs:end
