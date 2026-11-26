#!/usr/bin/env bash
set -euo pipefail

# docs:start
SCHEMA='{
  "type": "object",
  "properties": {
    "vendor_name": {"type": "string"},
    "invoice_number": {"type": "string"},
    "total_amount": {"type": "number"}
  },
  "required": ["total_amount"]
}'

# startRow and startCol are 0-based cell coordinates: 0 and 0 target cell A1
jq -n \
  --arg schema "$SCHEMA" \
  '{
    prompt: "Extract the vendor, invoice number and total of each invoice.",
    schema: $schema,
    excelTemplate: {startRow: 0, startCol: 0, includeHeaders: true, sheetName: "Data"}
  }' |
  curl -s -S -f -o /dev/null -w "Configuration saved (status %{http_code})\n" -X POST "https://api.docudevs.ai/configuration/invoice-config" \
    -H "Authorization: $API_KEY" \
    -H "Content-Type: application/json" \
    --data-binary @-
# docs:end
