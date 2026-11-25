#!/usr/bin/env bash
set -euo pipefail

# docs:start
GENERATION_JOB_GUID="<generation-job-guid>"  # jobGuid returned by the generate request

echo '{"hours": {"action": "accept_proposed"}}' > decisions.json

docudevs --token "$DOCUDEVS_API_KEY" knowledge-base contract-analysis generation-apply 42 "$GENERATION_JOB_GUID" \
  --decisions decisions.json
# docs:end
