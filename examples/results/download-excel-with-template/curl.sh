#!/usr/bin/env bash
set -euo pipefail

# docs:start
JOB_GUID="<job-guid>"  # GUID of a completed extraction job

# invoice-config is a saved configuration that has an Excel template attached.
# Name the output file .xlsm when the template is macro-enabled.
curl -s -S -f "https://api.docudevs.ai/job/result/$JOB_GUID/excel?configuration=invoice-config" \
  -H "Authorization: $API_KEY" \
  --output result.xlsx
# docs:end
