#!/usr/bin/env bash
set -uo pipefail

# docs:start
if ! CASE_ID=$(curl -s -S -f -X POST "https://api.docudevs.ai/cases" \
  -H "Authorization: $API_KEY" \
  -H "Content-Type: application/json" \
  -d '{"name": "Contract Review"}' | jq -r '.id'); then
  echo "Error creating case" >&2
  exit 1
fi
echo "Created case: $CASE_ID"

if ! curl -s -S -f -o /dev/null -X POST "https://api.docudevs.ai/cases/$CASE_ID/documents" \
  -H "Authorization: $API_KEY" \
  -F "document=@contract.pdf"; then
  echo "Error uploading document" >&2
  exit 1
fi
echo "Uploaded document to case"
# docs:end
