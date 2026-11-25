#!/usr/bin/env bash
set -euo pipefail

# docs:start
CASE_ID=$(curl -s -S -f -X POST "https://api.docudevs.ai/cases" \
  -H "Authorization: $API_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Q4 2024 Vendor Invoices",
    "description": "All invoices from vendors for Q4 2024 processing"
  }' | jq -r '.id')
echo "Created case ID: $CASE_ID"
# docs:end
