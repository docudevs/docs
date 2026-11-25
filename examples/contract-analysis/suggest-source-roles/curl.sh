#!/usr/bin/env bash
set -euo pipefail

# docs:start
CASE_ID=42  # id of the case that holds the playbook documents

curl -s -S -f -X POST "https://api.docudevs.ai/knowledge-bases/$CASE_ID/profiles/contract-analysis/source-authority/suggestions" \
  -H "Authorization: $API_KEY" | jq -r '"Suggestion job: \(.jobGuid)"'
# docs:end
