#!/usr/bin/env bash
set -euo pipefail

# docs:start
CASE_ID=42  # id of the case that holds the playbook documents
RUN_ID="<run-id>"  # runId returned when the run was created
RESULT_FINGERPRINT="<result-fingerprint>"  # resultFingerprint of that run's result
SUITE_REVISION=4  # revision returned by the test-suite read

curl -s -S -f -X POST "https://api.docudevs.ai/knowledge-bases/$CASE_ID/profiles/contract-analysis/test-suite/confirmations" \
  -H "Authorization: $API_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "runId": "'"$RUN_ID"'",
    "resultFingerprint": "'"$RESULT_FINGERPRINT"'",
    "expectedSuiteRevision": '"$SUITE_REVISION"',
    "confirmations": [
      {
        "clauseId": "term",
        "expectedVerdict": "flagged",
        "expectedRedFlags": {"auto-renewal": "triggered"},
        "importance": "mandatory",
        "notes": "Confirmed by review."
      }
    ]
  }' | jq -r '"Suite case: \(.suiteCaseId)"'
# docs:end
