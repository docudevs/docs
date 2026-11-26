#!/usr/bin/env bash
set -euo pipefail

# docs:start
CASE_ID=42  # id of the playbook's case

curl -s -S -f "https://api.docudevs.ai/knowledge-bases/$CASE_ID/profiles/contract-analysis/evaluations" \
  -H "Authorization: $API_KEY" \
  | jq -r '(.evaluations // [])[] | "\(.evaluationId) \(.set) \(if .complete then "complete" else "running" end)"'

EVALUATION_ID="<evaluation-id>"  # evaluationId returned when the evaluation was started

curl -s -S -f "https://api.docudevs.ai/knowledge-bases/$CASE_ID/profiles/contract-analysis/evaluations/$EVALUATION_ID" \
  -H "Authorization: $API_KEY" | jq -r '"Trials: \(.trialCounts // {} | tojson)"'

# Add ?against=<evaluation id> or ?against=published to compare with another evaluation
curl -s -S -f "https://api.docudevs.ai/knowledge-bases/$CASE_ID/profiles/contract-analysis/evaluations/$EVALUATION_ID/report" \
  -H "Authorization: $API_KEY" \
  | jq -r '"Complete: \(.complete)",
    "Passed \(.totals.passed) of \(.totals.assertions) assertions",
    "Missed problems: \(.totals.missedProblems // 0), false alarms: \(.totals.falseAlarms // 0)",
    ((.contracts // [])[] | select(.trialStatus != "finished") | "\(.name): \(.trialStatus) (\(.failureReason // "no reason"))")'
# docs:end
