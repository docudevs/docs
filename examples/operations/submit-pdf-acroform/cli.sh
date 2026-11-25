#!/usr/bin/env bash
set -euo pipefail

# docs:start
PARENT_JOB_GUID="<job-guid>"  # GUID of a completed PDF job whose page images exist (processed with ocr PREMIUM)

docudevs operations pdf-acroform "$PARENT_JOB_GUID" --no-wait
# docs:end
