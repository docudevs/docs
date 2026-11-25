#!/usr/bin/env bash
set -euo pipefail

# docs:start
CASE_ID=42  # id of the case that holds the playbook documents
TRIAL_ID="<trial-id>"  # trialId returned when the trial was launched

curl -s -S "https://api.docudevs.ai/knowledge-bases/$CASE_ID/profiles/contract-analysis/trials/$TRIAL_ID" \
  -H "Authorization: $API_KEY" | jq .
# docs:end
