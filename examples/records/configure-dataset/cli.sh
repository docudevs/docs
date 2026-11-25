#!/usr/bin/env bash
set -euo pipefail

# docs:start
CASE_ID=42  # id of a case that holds no documents

cat > dataset.json <<'JSON'
{
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
}
JSON

docudevs records dataset-set "$CASE_ID" dataset.json
# docs:end
