#!/usr/bin/env bash
set -euo pipefail

# docs:start
CASE_ID=42  # id of the playbook's case

wait_for_evaluation() {
  local deadline=$((SECONDS + 3600))
  while [ "$SECONDS" -lt "$deadline" ]; do
    STATUS=$(curl -s -S -f "https://api.docudevs.ai/knowledge-bases/$CASE_ID/profiles/contract-analysis/evaluations/$1" \
      -H "Authorization: $API_KEY")
    if [ "$(echo "$STATUS" | jq -r '.complete')" = "true" ]; then
      echo "Evaluation $1 complete: $(echo "$STATUS" | jq -c '.trialCounts // {}')"
      return 0
    fi
    sleep 15
  done
  echo "Evaluation $1 did not complete within 3600 seconds" >&2
  return 1
}

# Reuse the idempotency key to retry a start safely
STARTED=$(curl -s -S -f -X POST "https://api.docudevs.ai/knowledge-bases/$CASE_ID/profiles/contract-analysis/evaluations" \
  -H "Authorization: $API_KEY" \
  -H "Content-Type: application/json" \
  -d "$(jq -n --arg key "$(uuidgen)" '{set: "tuning", idempotencyKey: $key}')")
EVALUATION_ID=$(echo "$STARTED" | jq -r '.evaluationId')
echo "Evaluation $EVALUATION_ID started: $(echo "$STARTED" | jq '(.caseIds // []) | length') cases"

wait_for_evaluation "$EVALUATION_ID"
# docs:end
