#!/usr/bin/env bash
set -euo pipefail

# docs:start
PARENT_JOB_GUID="<job-guid>"  # GUID of a completed PDF job whose page images exist (processed with ocr PREMIUM)

docudevs operations pdf-acroform "$PARENT_JOB_GUID" \
  --ocr PREMIUM \
  --page 1 \
  --min-confidence 0.6 \
  --max-fields-per-page 300 \
  --timeout 600 \
  --output fillable.pdf
# docs:end
