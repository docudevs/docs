#!/usr/bin/env bash
set -euo pipefail

# docs:start
CASE_ID=42  # id of the case that holds the rows; it may differ from the catalogue

echo '[{"key": "line-1", "text": {"title": "M8 hex bolt 40 mm, 100 pcs"}}]' > rows.json
cat > config.json <<'JSON'
{
  "target": "record",
  "recordsKnowledgeBaseId": 51,
  "candidateStrategy": {"similarRecords": {"k": 20}},
  "instructions": "Match the invoice line to the catalogue product with the same item and pack size.",
  "routing": {"reviewWhen": ["declined", "modelNamedDifferentBestFit"]}
}
JSON

docudevs classify start "$CASE_ID" --rows-file rows.json --config-file config.json --no-wait
# docs:end
