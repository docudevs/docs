#!/usr/bin/env bash
set -euo pipefail

# docs:start
CASE_ID=42  # id of a records case
ENTRY_ID=m_88  # id returned when the entry was created

curl -s -S -f "https://api.docudevs.ai/cases/$CASE_ID/memo?status=active" \
  -H "Authorization: $API_KEY" | jq -r '.items[]? | "\(.id) \(.type) \(.status)"'

curl -s -S -f "https://api.docudevs.ai/cases/$CASE_ID/memo/entries/$ENTRY_ID/revisions" \
  -H "Authorization: $API_KEY" | jq -r '.items[]? | "revision \(.revision): \(.status)"'

# Retiring adds a revision; earlier versions stay readable
curl -s -S -f -X POST "https://api.docudevs.ai/cases/$CASE_ID/memo/entries/$ENTRY_ID/retire" \
  -H "Authorization: $API_KEY" | jq '{id, status, memoVersion}'
# docs:end
