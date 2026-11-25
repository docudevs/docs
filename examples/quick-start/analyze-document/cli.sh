#!/usr/bin/env bash
set -euo pipefail

# docs:start
docudevs ocr-only your-document.pdf \
  --ocr PREMIUM \
  --format markdown \
  --no-wait > ocr-submit.txt
OCR_JOB_ID=$(sed -n 's/.*GUID: //p' ocr-submit.txt)

docudevs wait "$OCR_JOB_ID" --timeout 180 > /dev/null

docudevs operations generative-task "$OCR_JOB_ID" \
  --prompt "Summarize this document and explain what type of document it is and its main purpose."
# docs:end
