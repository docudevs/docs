#!/usr/bin/env bash
set -euo pipefail

# docs:start
CASE_ID=42  # id of the case that holds the playbook documents

TRIAL_ID="<trial-id>"  # trialId returned when the trial was launched

docudevs knowledge-base contract-analysis trial-get "$CASE_ID" "$TRIAL_ID" > trial.json

docudevs knowledge-base contract-analysis trial-attest "$CASE_ID" "$TRIAL_ID" \
  --report-id "$(jq -r '.report.reportId' trial.json)" \
  --reason "Reviewed and correct"
# docs:end
