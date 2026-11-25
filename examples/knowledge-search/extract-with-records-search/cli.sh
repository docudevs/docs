#!/usr/bin/env bash
set -euo pipefail

# docs:start
CASE_ID=51  # id of the records knowledge base, from "docudevs knowledge-base list"

docudevs process invoice.pdf \
  --mime-type application/pdf \
  --prompt "For each invoice line, fill product_key with the catalogue record that matches the item." \
  --schema '{"type":"object","properties":{"lines":{"type":"array","items":{"type":"object","properties":{"description":{"type":"string"},"product_key":{"type":"string"}}}}}}' \
  --tool '{"type":"RECORDS_SEARCH","config":{"caseId":"'"$CASE_ID"'","topK":5}}'
# docs:end
