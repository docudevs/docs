#!/usr/bin/env bash
set -euo pipefail

# docs:start
JOB_GUID="<job-guid>"  # GUID of a completed job that was processed with a schema

curl -s -S -f "https://api.docudevs.ai/job/result/$JOB_GUID/schema" \
  -H "Authorization: $API_KEY" | jq .
# docs:end
