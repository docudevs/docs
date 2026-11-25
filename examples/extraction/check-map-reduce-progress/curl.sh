#!/usr/bin/env bash
set -euo pipefail

# docs:start
JOB_ID="<job-guid>"  # GUID of a running map-reduce job

curl -s -S -f "https://api.docudevs.ai/job/status/$JOB_ID" \
  -H "Authorization: $API_KEY" \
  | jq -r '"Progress: \(.mapReduceStatus.completedChunks) / \(.mapReduceStatus.totalChunks)"'
# docs:end
