#!/usr/bin/env bash
set -euo pipefail

# docs:start
CASE_ID=42  # id of a records case

# One page (limit 1 to 200, default 50); pass .continuation back as
# --data-urlencode "continuation=<token>" to read the next page. The Python
# client follows the tokens for you.
curl -s -S -f -G "https://api.docudevs.ai/cases/$CASE_ID/records" \
  -H "Authorization: $API_KEY" \
  --data-urlencode "subtree=4.12" \
  --data-urlencode "attribute=brand=Foo" | jq -r '(.records // [])[] | "\(.key) \(.label // "-")"'
# docs:end
