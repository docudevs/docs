#!/usr/bin/env bash
set -euo pipefail

# docs:start
JOB_GUID="<job-guid>"  # GUID of a completed job

curl -s -S -f "https://api.docudevs.ai/job/status/$JOB_GUID" \
  -H "Authorization: $API_KEY" | jq '{qualityScore, qualityCategory}'
# docs:end
