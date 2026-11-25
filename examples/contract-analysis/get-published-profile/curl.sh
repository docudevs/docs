#!/usr/bin/env bash
set -euo pipefail

# docs:start
CASE_ID=42  # id of the case that holds the playbook documents

curl -s -S -f "https://api.docudevs.ai/knowledge-bases/$CASE_ID/profiles/contract-analysis?view=published" \
  -H "Authorization: $API_KEY" | jq .
# docs:end
