#!/usr/bin/env bash
set -euo pipefail

# docs:start
# The portal wraps an existing processing configuration, here "invoice-extractor".
curl -s -S -f -X POST "https://api.docudevs.ai/submission-portals" \
  -H "Authorization: $API_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "slug": "invoices",
    "configurationName": "invoice-extractor",
    "title": "Submit an invoice",
    "inputMode": "SINGLE_FILE",
    "resultMode": "DOWNLOAD",
    "downloadFormat": "EXCEL",
    "isDefaultForConfiguration": true,
    "active": true
  }'
# docs:end
