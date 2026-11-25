#!/usr/bin/env bash
set -euo pipefail

# docs:start
CASE_ID=42  # id of the case that holds the rows; it may differ from the catalogue

# recordsKnowledgeBaseId is the id of the catalogue's records case
curl -s -S -f -X POST "https://api.docudevs.ai/cases/$CASE_ID/classify" \
  -H "Authorization: $API_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "rows": {
      "source": "inline",
      "items": [{"key": "line-1", "text": {"title": "M8 hex bolt 40 mm, 100 pcs"}}]
    },
    "config": {
      "target": "record",
      "recordsKnowledgeBaseId": 51,
      "candidateStrategy": {"similarRecords": {"k": 20}},
      "instructions": "Match the invoice line to the catalogue product with the same item and pack size.",
      "routing": {"reviewWhen": ["declined", "modelNamedDifferentBestFit"]}
    }
  }' | jq -r '"Classify job \(.jobId): \(.status)"'
# docs:end
