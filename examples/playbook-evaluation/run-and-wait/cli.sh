#!/usr/bin/env bash
set -euo pipefail

# docs:start
CASE_ID=42  # id of the playbook's case

docudevs eval run "$CASE_ID" --set tuning
# docs:end
