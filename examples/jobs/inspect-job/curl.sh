#!/usr/bin/env bash
set -euo pipefail

# docs:start
JOB_GUID="<job-guid>"  # GUID of a job that was submitted with dependsOn

# The processing options the job was submitted with
curl -s -S -f "https://api.docudevs.ai/job/params/$JOB_GUID" \
  -H "Authorization: $API_KEY" | jq .

# The job it depends on, and whether it may run
curl -s -S -f "https://api.docudevs.ai/job/dependencies/$JOB_GUID" \
  -H "Authorization: $API_KEY" \
  | jq '{dependsOnGuid, dependencyStatus, parentStatus: .parentJob.status}'
# docs:end
