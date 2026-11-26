#!/usr/bin/env bash
set -euo pipefail

# docs:start
# The file name must end in .docx
RESULT=$(curl -s -S -f -X POST "https://api.docudevs.ai/document/track-changes" \
  -H "Authorization: $API_KEY" \
  -F "document=@contract.docx")

echo "$RESULT" | jq '.summary'

echo "$RESULT" | jq -r '(.insertions // [])[] | "inserted by \(.author // "unknown") on \(.date // "unknown date"): \(.text)"'
echo "$RESULT" | jq -r '(.deletions // [])[] | "deleted by \(.author // "unknown") on \(.date // "unknown date"): \(.text)"'
echo "$RESULT" | jq -r '(.comments // [])[] | "comment by \(.author // "unknown") on \(.date // "unknown date"): \(.text)"'
# docs:end
