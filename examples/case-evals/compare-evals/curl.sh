#!/usr/bin/env bash
set -euo pipefail

# docs:start
CASE_ID=42  # id of a records case with an active category tree
BASELINE_EVAL_ID=301  # ids of two completed holdout evaluations
CANDIDATE_EVAL_ID=302  # that drew the same sample

wait_for_eval() {
  local deadline=$((SECONDS + 3600))  # same limit as the Python timeout
  while [ "$SECONDS" -lt "$deadline" ]; do
    STATUS=$(curl -s -S -f "https://api.docudevs.ai/cases/$CASE_ID/evals/$1" \
      -H "Authorization: $API_KEY" | jq -r '.status')
    case "$STATUS" in
      COMPLETED) return 0 ;;
      FAILED) echo "Evaluation $1 ended with status $STATUS" >&2; return 1 ;;
    esac
    sleep 5
  done
  echo "Evaluation $1 did not complete in time" >&2
  return 1
}

REQUEST=$(jq -n --argjson baseline "$BASELINE_EVAL_ID" --argjson candidate "$CANDIDATE_EVAL_ID" '{
  baselineEvalId: $baseline,
  candidateEvalId: $candidate,
  baselineVariant: "baseline",
  candidateVariant: "with-rule"
}')

# Scoring is asynchronous: the answer carries the id of a new evaluation
COMPARE_ID=$(curl -s -S -f -X POST "https://api.docudevs.ai/cases/$CASE_ID/evals:compare" \
  -H "Authorization: $API_KEY" \
  -H "Content-Type: application/json" \
  -d "$REQUEST" | jq -r '.evalId')

wait_for_eval "$COMPARE_ID"

curl -s -S -f "https://api.docudevs.ai/cases/$CASE_ID/evals/$COMPARE_ID" \
  -H "Authorization: $API_KEY" | jq '.report.comparisons[] | {baseline, candidate, target}'
# docs:end
