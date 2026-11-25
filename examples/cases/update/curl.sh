#!/usr/bin/env bash
set -euo pipefail

# docs:start
CASE_ID=123  # id returned when the case was created

curl -s -S -X PUT "https://api.docudevs.ai/cases/$CASE_ID" \
  -H "Authorization: $API_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Q4 2024 Vendor Invoices - Updated",
    "description": "Updated description with additional context"
  }' | jq .
# docs:end
