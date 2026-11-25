#!/usr/bin/env bash
set -euo pipefail

# docs:start
cat > data.json <<'EOF'
{
  "fields": {
    "customerName": "Acme Corp",
    "invoiceNumber": "INV-2024-001",
    "totalAmount": "1500.00",
    "paid": true
  }
}
EOF

docudevs fill invoice data.json --output filled_invoice.pdf
# docs:end
