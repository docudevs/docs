#!/usr/bin/env bash
set -euo pipefail

# docs:start
curl -s -S -f -X POST "https://api.docudevs.ai/configuration/invoice-config" \
  -H "Authorization: $API_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "prompt": "Extract invoice data including vendor details, line items, and totals.",
    "schema": "{\"type\": \"object\", \"properties\": {\"vendor_name\": {\"type\": \"string\"}, \"invoice_number\": {\"type\": \"string\"}, \"total_amount\": {\"type\": \"number\"}, \"date\": {\"type\": \"string\", \"format\": \"date\"}}, \"required\": [\"vendor_name\", \"total_amount\"]}",
    "ocr": "PREMIUM",
    "llm": "HIGH",
    "extractionMode": "STEPS",
    "barcodes": true
  }' | jq .
# docs:end
