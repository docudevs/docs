#!/usr/bin/env bash
set -euo pipefail

# docs:start
JOB_GUID="<job-guid>"  # GUID of a completed job

# Prints the full job status, which includes the quality score and category
docudevs status "$JOB_GUID"
# docs:end
