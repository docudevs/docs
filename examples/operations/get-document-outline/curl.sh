#!/usr/bin/env bash
set -euo pipefail

# docs:start
JOB_GUID="<job-guid>"  # GUID of a job that already has an outline artifact

curl -s -S -f "https://api.docudevs.ai/job/result/$JOB_GUID/document-outline" \
  -H "Authorization: $API_KEY" | jq .
# docs:end
