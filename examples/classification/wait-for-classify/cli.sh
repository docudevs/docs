#!/usr/bin/env bash
set -euo pipefail

# docs:start
CASE_ID=42  # id of a records case
JOB_ID=9  # jobId returned when the job was started

docudevs classify status "$CASE_ID" "$JOB_ID" --wait
# docs:end
