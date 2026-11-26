#!/usr/bin/env bash
set -euo pipefail

# docs:start
CASE_ID=42  # id of the playbook's case

docudevs eval import "$CASE_ID" --gold gold.xlsx --mapping mapping.yaml --dry-run
docudevs eval import "$CASE_ID" --gold gold.xlsx --mapping mapping.yaml
# docs:end
