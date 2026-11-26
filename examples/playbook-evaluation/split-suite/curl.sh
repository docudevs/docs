#!/usr/bin/env bash
set -euo pipefail

# docs:start
CASE_ID=42  # id of the playbook's case

REVISION=$(curl -s -S -f "https://api.docudevs.ai/knowledge-bases/$CASE_ID/profiles/contract-analysis/test-suite" \
  -H "Authorization: $API_KEY" | jq -r '.revision')

curl -s -S -f -X POST "https://api.docudevs.ai/knowledge-bases/$CASE_ID/profiles/contract-analysis/test-suite/split" \
  -H "Authorization: $API_KEY" \
  -H "Content-Type: application/json" \
  -d "$(jq -n --argjson revision "$REVISION" '{holdout: 0.3, seed: 1, expectedSuiteRevision: $revision}')" \
  | jq -r '(.cases // []) as $cases
    | ([$cases[] | select(.set == "tuning")] | length) as $tuning
    | ([$cases[] | select(.set == "holdout")] | length) as $holdout
    | "Suite revision \(.revision) saved: \($tuning) tuning, \($holdout) holdout, \(($cases | length) - $tuning - $holdout) unassigned."'
# docs:end
