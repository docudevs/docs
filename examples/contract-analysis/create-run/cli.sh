#!/usr/bin/env bash
set -euo pipefail

# docs:start
CASE_ID=42  # id of the case that holds the playbook documents

echo '[{"jobGuid": "<agreement-job>", "role": "agreement"}, {"jobGuid": "<amendment-job>", "role": "amendment"}]' > members.json

docudevs --token "$DOCUDEVS_API_KEY" knowledge-base contract-analysis run-create "$CASE_ID" \
  --anchor-job-guid "<agreement-job>" \
  --idempotency-key request-1 \
  --mode published \
  --package-members members.json
# docs:end
