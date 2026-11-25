#!/usr/bin/env bash
set -euo pipefail

# docs:start
CASE_ID=123  # id returned when the case was created

curl -s -S -f -o /dev/null -X DELETE "https://api.docudevs.ai/cases/$CASE_ID" \
  -H "Authorization: $DOCUDEVS_API_KEY"
echo "Case deleted successfully"
# docs:end
