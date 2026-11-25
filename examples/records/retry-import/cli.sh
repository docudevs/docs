#!/usr/bin/env bash
set -euo pipefail

# docs:start
CASE_ID=42  # id of a records case
IMPORT_ID=301  # id of the import that failed

docudevs records import-retry "$CASE_ID" "$IMPORT_ID"
# docs:end
