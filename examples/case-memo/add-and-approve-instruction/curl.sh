#!/usr/bin/env bash
set -euo pipefail

# docs:start
CASE_ID=42  # id of a records case

ENTRY_ID=$(curl -s -S -f -X POST "https://api.docudevs.ai/cases/$CASE_ID/memo/entries" \
  -H "Authorization: $API_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "type": "instruction",
    "scope": {"kind": "subtree", "code": "7.31"},
    "text": "Fiction books are filed by genre when the description names one."
  }' | jq -r '.id')
echo "Proposed entry $ENTRY_ID"

# Nothing changes in classification until the instruction is approved
curl -s -S -f -X POST "https://api.docudevs.ai/cases/$CASE_ID/memo/entries/$ENTRY_ID/approve" \
  -H "Authorization: $API_KEY" | jq '{id, status, memoVersion}'
# docs:end
