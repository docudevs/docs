#!/usr/bin/env bash
set -euo pipefail

# docs:start
CASE_ID=42  # id of a records case with a configured dataset

docudevs records bulk "$CASE_ID" products.csv --wait
# docs:end
