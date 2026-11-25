#!/usr/bin/env bash
set -euo pipefail

# docs:start
CASE_ID=42  # id of a records case with a configured dataset

docudevs records import-url "$CASE_ID" "https://example.com/exports/products.csv" --format csv --wait
# docs:end
