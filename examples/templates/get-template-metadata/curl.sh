#!/usr/bin/env bash
set -euo pipefail

# docs:start
# The metadata endpoint answers 202 while PDF field extraction is pending
while true; do
  STATUS=$(curl -s -S -o metadata.json -w "%{http_code}" "https://api.docudevs.ai/template/metadata/invoice" \
    -H "Authorization: $API_KEY")
  if [ "$STATUS" = "200" ]; then break; fi
  if [ "$STATUS" != "202" ]; then echo "Unexpected status $STATUS" >&2; exit 1; fi
  sleep 1
done

jq -r '.[] | "- \(.name) (\(.type))"' metadata.json
# docs:end
