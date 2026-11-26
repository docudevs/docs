#!/usr/bin/env bash
set -euo pipefail

# docs:start
# Two commands: create the case, then configure its records dataset.
docudevs cases create \
  --name "Product catalogue" \
  --description "Reference products for matching"

CASE_ID=42  # id of the case printed by the previous command

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
  "embeddedText": {"template": "{title}\n{description}", "skipEmptyFields": true},
  "indexSettings": {"topK": 10, "hybrid": false}
}
JSON

docudevs records dataset-set "$CASE_ID" dataset.json
# docs:end
