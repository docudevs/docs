#!/usr/bin/env bash
set -euo pipefail

# docs:start
# Step 1: Create a case first
CASE_ID=$(curl -s -S -f -X POST "https://api.docudevs.ai/cases" \
  -H "Authorization: $API_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Contract Review",
    "description": "Contract documents for review"
  }' | jq -r '.id')

# Step 2: Upload document to case
curl -s -S -f -o /dev/null -X POST "https://api.docudevs.ai/cases/$CASE_ID/documents" \
  -H "Authorization: $API_KEY" \
  -F "document=@contract.pdf"

echo "Uploaded document to case $CASE_ID"
# docs:end
