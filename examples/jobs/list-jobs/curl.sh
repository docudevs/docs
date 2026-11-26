#!/usr/bin/env bash
set -euo pipefail

# docs:start
# Completed jobs created since 2025-01-01, newest first, 50 per page
PAGE=0
while true; do
  RESPONSE=$(curl -s -S -f -G "https://api.docudevs.ai/job/list" \
    -H "Authorization: $API_KEY" \
    --data-urlencode "status=COMPLETED" \
    --data-urlencode "createdAtFrom=2025-01-01" \
    --data-urlencode "sort=createdAt,desc" \
    --data-urlencode "size=50" \
    --data-urlencode "page=$PAGE")

  echo "$RESPONSE" | jq -r '(.content // [])[] | "\(.guid) \(.status) \(.createdAt)"'

  TOTAL_PAGES=$(echo "$RESPONSE" | jq -r '.totalPages // 0')
  PAGE=$((PAGE + 1))
  if [ "$PAGE" -ge "$TOTAL_PAGES" ]; then
    break
  fi
done
# docs:end
