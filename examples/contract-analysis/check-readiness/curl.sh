#!/usr/bin/env bash
set -euo pipefail

# docs:start
CASE_ID=42  # id of the case that holds the playbook documents

curl -s -S -f "https://api.docudevs.ai/knowledge-bases/$CASE_ID/profiles/contract-analysis/readiness" \
  -H "Authorization: $API_KEY" | jq -r '"Ready: \(.ready)", "Blockers: \(.blockers)", "Scenarios still missing: \(.outstandingScenarios // [])"'
# docs:end
