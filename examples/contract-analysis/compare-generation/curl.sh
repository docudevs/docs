#!/usr/bin/env bash
set -euo pipefail

# docs:start
CASE_ID=42  # id of the case that holds the playbook documents
GENERATION_JOB_GUID="<generation-job-guid>"  # jobGuid returned by the generate request

curl -s -S "https://api.docudevs.ai/knowledge-bases/$CASE_ID/profiles/contract-analysis/generations/$GENERATION_JOB_GUID/comparison" \
  -H "Authorization: $API_KEY" | jq .
# docs:end
