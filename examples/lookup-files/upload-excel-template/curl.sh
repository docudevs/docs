#!/usr/bin/env bash
set -euo pipefail

# docs:start
curl -s -S -f -o /dev/null -w "Upload status: %{http_code}\n" -X POST "https://api.docudevs.ai/configuration/invoice-config/excel-template" \
  -H "Authorization: $API_KEY" \
  -F "file=@template.xlsx"
# docs:end
