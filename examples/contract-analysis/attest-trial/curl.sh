#!/usr/bin/env bash
set -euo pipefail

# docs:start
CASE_ID=42  # id of the case that holds the playbook documents
TRIAL_ID="<trial-id>"  # trialId returned when the trial was launched

REPORT_ID=$(curl -s -S "https://api.docudevs.ai/knowledge-bases/$CASE_ID/profiles/contract-analysis/trials/$TRIAL_ID" \
  -H "Authorization: $DOCUDEVS_API_KEY" | jq -r '.report.reportId')

curl -s -S -X POST "https://api.docudevs.ai/knowledge-bases/$CASE_ID/profiles/contract-analysis/trials/$TRIAL_ID/attest" \
  -H "Authorization: $DOCUDEVS_API_KEY" \
  -H "Content-Type: application/json" \
  -d '{"reportId": "'"$REPORT_ID"'", "reason": "Reviewed and correct"}' | jq .
# docs:end
