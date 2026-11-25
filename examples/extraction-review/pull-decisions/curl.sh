#!/usr/bin/env bash
set -euo pipefail

# docs:start
CURSOR=0  # nextCursor from the previous pull; 0 for the first pull

PAGE=$(curl -s -S -f "https://api.docudevs.ai/extraction-review-decisions?afterOrdinal=$CURSOR&limit=100" \
  -H "Authorization: $API_KEY")

echo "$PAGE" | jq -r '.events[] | "\(.cursorOrdinal) \(.eventId) \(.kind)"'

# Persist nextCursor exactly as returned, even when events is empty
CURSOR=$(echo "$PAGE" | jq -r '.nextCursor')
echo "Next cursor: $CURSOR"
# docs:end
