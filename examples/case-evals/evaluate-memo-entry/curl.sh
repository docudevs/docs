#!/usr/bin/env bash
set -euo pipefail

# docs:start
CASE_ID=42  # id of a records case with an active category tree
ENTRY_ID=m_88  # id of a proposed memo entry

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

EVAL_ID=$(curl -s -S -f -X POST "https://api.docudevs.ai/cases/$CASE_ID/memo/entries/$ENTRY_ID/evaluate" \
  -H "Authorization: $API_KEY" \
  -H "Content-Type: application/json" \
  -d '{"sampleSize": 200, "seed": 7, "groupField": "base_product_id"}' | jq -r '.evalId')

wait_for_eval "$EVAL_ID"

curl -s -S -f "https://api.docudevs.ai/cases/$CASE_ID/evals/$EVAL_ID" \
  -H "Authorization: $API_KEY" | jq '.report.comparisons[] | {baseline, candidate, target}'
# docs:end
