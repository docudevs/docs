#!/usr/bin/env bash
set -euo pipefail

# docs:start
JOB_GUID="<job-guid>"  # GUID of a completed job

# Get the thumbnail of page 0
curl -s -S -f "https://api.docudevs.ai/job/image/$JOB_GUID/0" \
  -H "Authorization: $API_KEY" \
  --output page_0.png
# docs:end
