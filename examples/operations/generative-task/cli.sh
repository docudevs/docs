#!/usr/bin/env bash
set -euo pipefail

# docs:start
PARENT_JOB_GUID="<job-guid>"  # GUID of a completed document job

docudevs operations generative-task "$PARENT_JOB_GUID" \
  --prompt "Summarize the main findings" \
  --model DEFAULT \
  --timeout 120

docudevs operations result "$PARENT_JOB_GUID" --type generative-task
# docs:end
