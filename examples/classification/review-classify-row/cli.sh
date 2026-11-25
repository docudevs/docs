#!/usr/bin/env bash
set -euo pipefail

# docs:start
CASE_ID=42  # id of a records case
JOB_ID=9  # jobId returned when the job was started
ROW_ID=17  # rowId of a result row

docudevs classify review "$CASE_ID" "$JOB_ID" "$ROW_ID" correct \
  --code 4.12.3.1 \
  --reason "Plain cotton sock, no sport use."
# docs:end
