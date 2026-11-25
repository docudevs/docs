#!/usr/bin/env bash
set -euo pipefail

# docs:start
CASE_ID=42  # id of a records case
IMPORT_ID=301  # importId returned when the import was created

docudevs records import-status "$CASE_ID" "$IMPORT_ID" --wait
# docs:end
