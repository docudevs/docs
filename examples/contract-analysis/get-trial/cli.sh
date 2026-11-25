#!/usr/bin/env bash
set -euo pipefail

# docs:start
TRIAL_ID="<trial-id>"  # trialId returned when the trial was launched

docudevs --token "$DOCUDEVS_API_KEY" knowledge-base contract-analysis trial-get 42 "$TRIAL_ID"
# docs:end
