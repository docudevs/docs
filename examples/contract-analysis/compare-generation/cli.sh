#!/usr/bin/env bash
set -euo pipefail

# docs:start
GENERATION_JOB_GUID="<generation-job-guid>"  # jobGuid returned by the generate request

docudevs --token "$DOCUDEVS_API_KEY" knowledge-base contract-analysis generation-compare 42 "$GENERATION_JOB_GUID"
# docs:end
