#!/usr/bin/env bash
set -euo pipefail

# docs:start
CASE_ID=42  # id of the playbook's case
# The CLI prints the whole status and report as JSON; the Python and cURL variants print a summary

docudevs eval list "$CASE_ID"

EVALUATION_ID="<evaluation-id>"  # evaluationId returned when the evaluation was started

docudevs eval status "$CASE_ID" "$EVALUATION_ID"

# Add --against <evaluation id> or --against published to compare with another evaluation
docudevs eval report "$CASE_ID" "$EVALUATION_ID" --format json
# docs:end
