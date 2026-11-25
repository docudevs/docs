#!/usr/bin/env bash
set -euo pipefail

# docs:start
wait_for_job() {
  local deadline=$((SECONDS + 600))
  while [ "$SECONDS" -lt "$deadline" ]; do
    STATUS=$(curl -s -S -f "https://api.docudevs.ai/job/status/$1" \
      -H "Authorization: $API_KEY" | jq -r '.status')
    case "$STATUS" in
      COMPLETED) return 0 ;;
      PARTIAL) echo "Job $1 ended with status PARTIAL" >&2; return 0 ;;
      ERROR|FAILED|TIMEOUT) echo "Job $1 ended with status $STATUS" >&2; return 1 ;;
    esac
    sleep 5
  done
  echo "Job $1 did not finish within 600 seconds" >&2
  return 1
}

BATCH_GUID=$(curl -s -S -f -X POST "https://api.docudevs.ai/document/batch" \
  -H "Authorization: $API_KEY" \
  -H "Content-Type: application/json" \
  -d '{"maxConcurrency": 3}' | jq -r '.jobGuid')

for FILENAME in invoice1.pdf invoice2.pdf; do
  curl -s -S -f -o /dev/null -X POST "https://api.docudevs.ai/document/batch/$BATCH_GUID/upload" \
    -H "Authorization: $API_KEY" \
    -F "document=@$FILENAME"
done

# The batch lookup file overrides the lookup file of invoice-config for this batch
curl -s -S -f -o /dev/null -X POST "https://api.docudevs.ai/document/batch/$BATCH_GUID/lookup-file" \
  -H "Authorization: $API_KEY" \
  -F "file=@vendors-2024.csv"

curl -s -S -f -o /dev/null -X POST "https://api.docudevs.ai/document/batch/$BATCH_GUID/process/with-configuration/invoice-config" \
  -H "Authorization: $API_KEY"

wait_for_job "$BATCH_GUID"

curl -s -S -f "https://api.docudevs.ai/job/result/$BATCH_GUID/json" \
  -H "Authorization: $API_KEY" | jq .
# docs:end
