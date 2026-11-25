#!/usr/bin/env bash
set -euo pipefail

# docs:start
CASE_ID=42  # id of a case that holds no documents

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
    "embeddedText": {"template": "{title}\n{description}"},
    "indexSettings": {"topK": 10, "hybrid": false}
  }' | jq '{datasetType, mappingVersion}'
# docs:end
