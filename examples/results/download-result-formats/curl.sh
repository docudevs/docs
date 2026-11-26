#!/usr/bin/env bash
set -euo pipefail

# docs:start
JOB_GUID="<job-guid>"  # GUID of a completed extraction job

# JSON: the stored result as it is
curl -s -S -f "https://api.docudevs.ai/job/result/$JOB_GUID/json" \
  -H "Authorization: $API_KEY" \
  --output result.json

# CSV: one row per record, nested fields as dotted column names
curl -s -S -f "https://api.docudevs.ai/job/result/$JOB_GUID/csv" \
  -H "Authorization: $API_KEY" \
  --output result.csv

# Excel: the same table as a workbook
curl -s -S -f "https://api.docudevs.ai/job/result/$JOB_GUID/excel" \
  -H "Authorization: $API_KEY" \
  --output result.xlsx
# docs:end
