#!/usr/bin/env bash
set -uo pipefail

# docs:start
JOB_GUID="<job-guid>"  # GUID of the job to delete

HTTP_STATUS=$(curl -s -S -o response.json -w "%{http_code}" -X DELETE "https://api.docudevs.ai/job/$JOB_GUID" \
  -H "Authorization: $API_KEY")

case "$HTTP_STATUS" in
  200)
    echo "Deleted successfully: $(jq '.jobsDeleted' response.json) job(s)"
    ;;
  404)
    echo "Job not found - may already be deleted or purged"
    ;;
  400)
    echo "Cannot delete: $(jq -r '.message // ""' response.json)"  # e.g., job still processing
    ;;
  *)
    echo "Unexpected status: $HTTP_STATUS"
    ;;
esac
# docs:end
