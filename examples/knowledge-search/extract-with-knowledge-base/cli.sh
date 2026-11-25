#!/usr/bin/env bash
set -euo pipefail

# docs:start
CASE_ID=42  # id of the knowledge base, from "docudevs knowledge-base list"

docudevs process products.csv \
  --mime-type text/csv \
  --prompt "Fill canonical_name, localized_display_name (at most 40 characters), product_group and marketing_summary for every SKU, using the Product Catalogue knowledge base." \
  --schema '{"type":"object","properties":{"records":{"type":"array","items":{"type":"object","properties":{"sku":{"type":"string"},"canonical_name":{"type":"string"},"localized_display_name":{"type":"string","maxLength":40},"product_group":{"type":"string"},"marketing_summary":{"type":"string"}},"required":["sku","canonical_name","localized_display_name"]}}},"required":["records"]}' \
  --tool '{"type":"KNOWLEDGE_BASE_SEARCH","config":{"caseId":"'"$CASE_ID"'","topK":8}}'
# docs:end
