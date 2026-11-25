#!/usr/bin/env bash
set -euo pipefail

# docs:start
curl -s -S -f -X POST "https://api.docudevs.ai/template/fill/invoice" \
  -H "Authorization: $API_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "fields": {
      "customerName": "Acme Corp",
      "invoiceNumber": "INV-2024-001",
      "totalAmount": "1500.00",
      "paid": true
    }
  }' \
  --output filled_invoice.pdf
echo "Saved filled_invoice.pdf"
# docs:end
