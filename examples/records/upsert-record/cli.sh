#!/usr/bin/env bash
set -euo pipefail

# docs:start
CASE_ID=42  # id of a records case

docudevs records upsert "$CASE_ID" 6410000123456 \
  --text title="Soft sock 3-pack" \
  --text description="Cotton, size 40-45" \
  --label 4.12.3 \
  --metadata brand=Foo
# docs:end
