#!/usr/bin/env bash
set -euo pipefail

# docs:start
SCHEMA='{
  "type": "object",
  "properties": {
    "invoice_number": {"type": "string"},
    "date": {"type": "string", "format": "date"},
    "vendor": {"type": "string"},
    "total": {"type": "number"},
    "currency": {"type": "string"}
  },
  "required": ["invoice_number", "total"]
}'

docudevs process your-document.pdf \
  --prompt "Extract invoice data according to the schema" \
  --schema "$SCHEMA"
# docs:end
