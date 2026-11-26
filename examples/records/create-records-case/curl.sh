#!/usr/bin/env bash
set -euo pipefail

# docs:start
# Two requests: create the case, then configure its records dataset.
CASE_ID=$(curl -s -S -f -X POST "https://api.docudevs.ai/cases" \
  -H "Authorization: $API_KEY" \
  -H "Content-Type: application/json" \
  -d '{"name": "Product catalogue", "description": "Reference products for matching"}' | jq -r '.id')

curl -s -S -f -X PUT "https://api.docudevs.ai/cases/$CASE_ID/dataset" \
  -H "Authorization: $API_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "datasetType": "records",
    "fieldMapping": {
      "key": "sku",
      "text": [
        {"column": "title", "name": "title"},
        {"column": "desc", "name": "description"}
      ],
      "label": "category",
      "metadata": {"brand": "brand"}
    },
    "embeddedText": {"template": "{title}\n{description}", "skipEmptyFields": true},
    "indexSettings": {"topK": 10, "hybrid": false}
  }' > /dev/null

echo "Records case $CASE_ID"
# docs:end
