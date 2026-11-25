#!/usr/bin/env bash
set -euo pipefail

# docs:start
JOB_GUID="<job-guid>"  # GUID of a job processed with the saved configuration

curl -s -S -f "https://api.docudevs.ai/job/$JOB_GUID/extraction-review-runs" \
  -H "Authorization: $API_KEY" | jq '.[] | {runId, availability, profileRevision, createdAt}'

RUN_ID="<run-id>"  # runId of the descriptor you selected from the list above

curl -s -S -f -X POST "https://api.docudevs.ai/extraction-review-runs/$RUN_ID/registration" \
  -H "Authorization: $API_KEY" | jq '{identity, reviewRevision}'
# docs:end
