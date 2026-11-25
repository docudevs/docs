#!/usr/bin/env bash
set -euo pipefail

# docs:start
CASE_ID=42  # id of the case that holds the playbook documents

GENERATION_JOB_GUID="<generation-job-guid>"  # jobGuid returned by the generate request

echo '{"hours": {"action": "accept_proposed"}}' > decisions.json

docudevs --token "$DOCUDEVS_API_KEY" knowledge-base contract-analysis generation-apply "$CASE_ID" "$GENERATION_JOB_GUID" \
  --decisions decisions.json
# docs:end
