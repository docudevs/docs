#!/usr/bin/env bash
set -euo pipefail

# docs:start
CASE_ID=42  # id of a records case

docudevs records get "$CASE_ID" 6410000123456
# docs:end
