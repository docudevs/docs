#!/usr/bin/env bash
set -euo pipefail

# docs:start
CASE_ID=42  # id of the case that holds the playbook documents

curl -s -S "https://api.docudevs.ai/knowledge-bases/$CASE_ID/profiles/contract-analysis/test-suite" \
  -H "Authorization: $DOCUDEVS_API_KEY" > suite.json

jq '{revision: .revision, cases: .cases}' suite.json > suite-update.json
# add or edit cases in suite-update.json, then save the next revision

curl -s -S -X PUT "https://api.docudevs.ai/knowledge-bases/$CASE_ID/profiles/contract-analysis/test-suite" \
  -H "Authorization: $DOCUDEVS_API_KEY" \
  -H "Content-Type: application/json" \
  -d @suite-update.json | jq .
# docs:end
