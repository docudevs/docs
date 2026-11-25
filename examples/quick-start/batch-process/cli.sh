#!/usr/bin/env bash
set -euo pipefail

# docs:start
SCHEMA='{
  "type": "object",
  "properties": {
    "invoice_number": {"type": "string"},
    "total": {"type": "number"}
  }
}'

docudevs --token "$DOCUDEVS_API_KEY" batch process invoice1.pdf invoice2.pdf invoice3.pdf \
  --prompt "Extract invoice data" \
  --schema "$SCHEMA" \
  --mime-type application/pdf \
  --max-concurrency 3 \
  --format json \
  --output results.json
# docs:end
