#!/usr/bin/env bash
set -euo pipefail

# docs:start
curl -s -S -f -X POST "https://api.docudevs.ai/document/ocr/sync?format=markdown&ocr=AUTO" \
  -H "Authorization: $API_KEY" \
  -F "document=@document.pdf"
# docs:end
