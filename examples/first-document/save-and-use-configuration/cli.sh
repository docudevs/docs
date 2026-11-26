#!/usr/bin/env bash
set -euo pipefail

# docs:start
SCHEMA='{
  "type": "object",
  "properties": {
    "invoice_number": {"type": "string"},
    "date": {"type": "string", "format": "date"},
    "vendor": {"type": "string"},
    "total": {"type": "number"}
  },
  "required": ["total"]
}'

# The configuration file holds the processing settings
jq -n \
  --arg schema "$SCHEMA" \
  '{prompt: "Extract comprehensive invoice data", schema: $schema, llm: "HIGH"}' > config.json

docudevs save-configuration invoice_processing_standard config.json

docudevs process sample_invoice.pdf --configuration invoice_processing_standard
# docs:end
