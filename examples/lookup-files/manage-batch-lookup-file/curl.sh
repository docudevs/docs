#!/usr/bin/env bash
set -euo pipefail

# docs:start
BATCH_GUID="<batch-guid>"  # GUID of a batch whose documents are uploaded and not yet processed

curl -s -S -f -o /dev/null -w "Upload status: %{http_code}\n" -X POST "https://api.docudevs.ai/document/batch/$BATCH_GUID/lookup-file" \
  -H "Authorization: $API_KEY" \
  -F "file=@vendors-2024.csv"

curl -s -S -f -o vendors-downloaded.csv "https://api.docudevs.ai/document/batch/$BATCH_GUID/lookup-file" \
  -H "Authorization: $API_KEY"

curl -s -S -f -o /dev/null -w "Delete status: %{http_code}\n" -X DELETE "https://api.docudevs.ai/document/batch/$BATCH_GUID/lookup-file" \
  -H "Authorization: $API_KEY"
# docs:end
