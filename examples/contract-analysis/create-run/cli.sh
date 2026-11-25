#!/usr/bin/env bash
set -euo pipefail

# docs:start
echo '[{"jobGuid": "<agreement-job>", "role": "agreement"}, {"jobGuid": "<amendment-job>", "role": "amendment"}]' > members.json

docudevs --token "$DOCUDEVS_API_KEY" knowledge-base contract-analysis run-create 42 \
  --anchor-job-guid "<agreement-job>" \
  --idempotency-key request-1 \
  --mode published \
  --package-members members.json
# docs:end
