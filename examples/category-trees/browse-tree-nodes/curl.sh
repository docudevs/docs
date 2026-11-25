#!/usr/bin/env bash
set -euo pipefail

# docs:start
CASE_ID=42  # id of a records case
VERSION_ID=7  # treeVersionId returned when the tree was uploaded

ROOTS=$(curl -s -S -f "https://api.docudevs.ai/cases/$CASE_ID/trees/$VERSION_ID/nodes" \
  -H "Authorization: $API_KEY")
CHILDREN=$(curl -s -S -f -G "https://api.docudevs.ai/cases/$CASE_ID/trees/$VERSION_ID/nodes" \
  -H "Authorization: $API_KEY" \
  --data-urlencode "parent=4.12")
HITS=$(curl -s -S -f -G "https://api.docudevs.ai/cases/$CASE_ID/trees/$VERSION_ID/nodes" \
  -H "Authorization: $API_KEY" \
  --data-urlencode "q=sock")

echo "$HITS" | jq -r '(.items // [])[] | "\(.code) \(.name) \(.status) \(.childCount)"'
echo "$HITS" | jq -r '"\(.total) matches"'
echo "$ROOTS" | jq -r '"\(.items // [] | length) roots"'
echo "$CHILDREN" | jq -r '"\(.items // [] | length) children of 4.12"'
# docs:end
