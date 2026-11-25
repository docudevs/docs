#!/usr/bin/env bash
set -euo pipefail

# docs:start
JOB_GUID="<job-guid>"  # GUID of a completed job that was processed with OCR

# Only page 1, and only its words and paragraphs
curl -s -S -f "https://api.docudevs.ai/job/result/$JOB_GUID/layout?pages=1&include=words,paragraphs" \
  -H "Authorization: $API_KEY" | jq .

curl -s -S -f "https://api.docudevs.ai/job/result/$JOB_GUID/ocr-quality" \
  -H "Authorization: $API_KEY" | jq .
# docs:end
