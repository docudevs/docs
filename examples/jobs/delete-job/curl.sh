#!/usr/bin/env bash
set -euo pipefail

# docs:start
JOB_GUID="<job-guid>"  # GUID of a completed job

curl -s -S -f -X DELETE "https://api.docudevs.ai/job/$JOB_GUID" \
  -H "Authorization: $API_KEY" | jq -r '"Deleted \(.jobsDeleted) job(s)"'
# docs:end
