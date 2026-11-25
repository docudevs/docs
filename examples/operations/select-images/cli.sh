#!/usr/bin/env bash
set -euo pipefail

# docs:start
JOB_GUID="<job-guid>"  # GUID of a completed job processed with figure extraction

docudevs operations submit "$JOB_GUID" \
  --type image-select \
  --parameter prompt="Return all diagrams from the document" \
  --parameter topK=5 \
  --parameter matchMode=all \
  --parameter useVision=false \
  --wait

docudevs operations result "$JOB_GUID" --type image-select
# docs:end
