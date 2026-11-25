#!/usr/bin/env bash
set -euo pipefail

# docs:start
OPERATION_JOB_GUID="<operation-job-guid>"  # operation job GUID returned when the conversion was submitted

curl -s -S -f "https://api.docudevs.ai/job/result/$OPERATION_JOB_GUID/pdf-acroform" \
  -H "Authorization: $API_KEY" \
  --output fillable.pdf
# docs:end
