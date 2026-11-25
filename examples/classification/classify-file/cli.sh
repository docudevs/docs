#!/usr/bin/env bash
set -euo pipefail

# docs:start
CASE_ID=42  # id of a records case with an active category tree

echo '{"target": "category", "llmLevel": "MINI"}' > config.json

docudevs classify start "$CASE_ID" \
  --file weekly.csv \
  --max-total-tokens 5000000 \
  --config-file config.json \
  --wait
# docs:end
