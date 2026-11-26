#!/usr/bin/env bash
set -euo pipefail

# docs:start
CASE_ID=42  # id of a knowledge base

curl -s -S -f -o /dev/null -X DELETE "https://api.docudevs.ai/knowledge-bases/$CASE_ID" \
  -H "Authorization: $API_KEY"
echo "Case $CASE_ID is no longer a knowledge base"
# docs:end
