#!/usr/bin/env bash
set -euo pipefail

# docs:start
# The CLI has no option for parallel processing.
docudevs process-map-reduce large-invoice.pdf \
  --prompt "Extract invoice line items (sku, description, quantity, unitPrice, total)." \
  --pages-per-chunk 4 \
  --overlap 1 \
  --dedup-key "sku" \
  --header-page-limit 1 \
  --header-row-prompt-augmentation "This is an invoice from Acme Corp." \
  --header-schema '{"type": "object", "properties": {"invoiceNumber": {"type": "string"}, "issueDate": {"type": "string"}, "billingAddress": {"type": "string"}}}'
# docs:end
