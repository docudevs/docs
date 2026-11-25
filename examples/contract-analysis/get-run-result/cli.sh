#!/usr/bin/env bash
set -euo pipefail

# docs:start
RUN_ID="<run-id>"  # runId returned when the run was created

docudevs knowledge-base contract-analysis run-result "$RUN_ID"
# docs:end
