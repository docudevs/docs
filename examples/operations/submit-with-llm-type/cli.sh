#!/usr/bin/env bash
set -euo pipefail

# docs:start
JOB_GUID="<job-guid>"  # GUID of a completed document job

docudevs operations error-analysis "$JOB_GUID" --llm-type HIGH --no-wait
# docs:end
