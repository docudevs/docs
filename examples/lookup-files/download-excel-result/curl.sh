#!/usr/bin/env bash
set -euo pipefail

# docs:start
JOB_GUID="<job-guid>"  # GUID of a completed extraction job

curl -s -S -f -o invoices.xlsx "https://api.docudevs.ai/job/result/$JOB_GUID/excel?configuration=invoice-config" \
  -H "Authorization: $API_KEY"
# docs:end
