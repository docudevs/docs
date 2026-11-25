#!/usr/bin/env bash
set -euo pipefail

# docs:start
docudevs --token "$DOCUDEVS_API_KEY" ocr-only your-document.pdf \
  --ocr PREMIUM \
  --format markdown

# Use the GUID printed by the previous command
OCR_JOB_ID="<guid-from-ocr-only>"

docudevs --token "$DOCUDEVS_API_KEY" operations generative-task "$OCR_JOB_ID" \
  --prompt "Summarize this document and explain what type of document it is and its main purpose."
# docs:end
