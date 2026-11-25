#!/usr/bin/env bash
set -euo pipefail

# docs:start
cat > config.json <<'EOF'
{
  "prompt": "Extract invoice data including vendor details, line items, and totals.",
  "schema": "{\"type\": \"object\", \"properties\": {\"vendor_name\": {\"type\": \"string\"}, \"invoice_number\": {\"type\": \"string\"}, \"total_amount\": {\"type\": \"number\"}, \"date\": {\"type\": \"string\", \"format\": \"date\"}}, \"required\": [\"vendor_name\", \"total_amount\"]}",
  "ocr": "PREMIUM",
  "llm": "HIGH",
  "extractionMode": "STEPS",
  "barcodes": true
}
EOF

docudevs save-configuration invoice-config config.json
# docs:end
