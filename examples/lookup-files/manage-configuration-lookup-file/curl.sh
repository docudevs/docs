#!/usr/bin/env bash
set -euo pipefail

# docs:start
curl -s -S -f -o /dev/null -w "Upload status: %{http_code}\n" -X POST "https://api.docudevs.ai/configuration/invoice-config/lookup-file" \
  -H "Authorization: $API_KEY" \
  -F "file=@vendors.csv"

curl -s -S -f -o vendors-downloaded.csv "https://api.docudevs.ai/configuration/invoice-config/lookup-file" \
  -H "Authorization: $API_KEY"

curl -s -S -f -o /dev/null -w "Delete status: %{http_code}\n" -X DELETE "https://api.docudevs.ai/configuration/invoice-config/lookup-file" \
  -H "Authorization: $API_KEY"
# docs:end
