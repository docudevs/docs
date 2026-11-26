#!/usr/bin/env bash
set -euo pipefail

# docs:start
CASE_ID=42  # id of the playbook's case

docudevs eval split "$CASE_ID" --holdout 0.3 --seed 1
# docs:end
