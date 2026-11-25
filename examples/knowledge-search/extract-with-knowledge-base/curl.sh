#!/usr/bin/env bash
set -euo pipefail

# docs:start
wait_for_job() {
  local deadline=$((SECONDS + 600))
  while [ "$SECONDS" -lt "$deadline" ]; do
    STATUS=$(curl -s -S -f "https://api.docudevs.ai/job/status/$1" \
      -H "Authorization: $API_KEY" | jq -r '.status')
    case "$STATUS" in
      COMPLETED) return 0 ;;
      PARTIAL) echo "Job $1 ended with status PARTIAL" >&2; return 0 ;;
      ERROR|FAILED|TIMEOUT) echo "Job $1 ended with status $STATUS" >&2; return 1 ;;
    esac
    sleep 5
  done
  echo "Job $1 did not finish within 600 seconds" >&2
  return 1
}

CASE_ID=42  # id of the knowledge base, from GET /knowledge-bases

SCHEMA='{
  "type": "object",
  "properties": {
    "records": {
      "type": "array",
      "items": {
        "type": "object",
        "properties": {
          "sku": {"type": "string"},
          "canonical_name": {"type": "string"},
          "localized_display_name": {"type": "string", "maxLength": 40},
          "product_group": {"type": "string"},
          "marketing_summary": {"type": "string"}
        },
        "required": ["sku", "canonical_name", "localized_display_name"]
      }
    }
  },
  "required": ["records"]
}'

JOB_GUID=$(curl -s -S -f -X POST "https://api.docudevs.ai/document/upload" \
  -H "Authorization: $API_KEY" \
  -F "document=@products.csv" | jq -r '.guid')

jq -n \
  --arg schema "$SCHEMA" \
  --arg caseId "$CASE_ID" \
  '{
    mimeType: "text/csv",
    prompt: "Fill canonical_name, localized_display_name (at most 40 characters), product_group and marketing_summary for every SKU, using the Product Catalogue knowledge base.",
    schema: $schema,
    tools: [{type: "KNOWLEDGE_BASE_SEARCH", config: {caseId: $caseId, topK: 8}}]
  }' |
  curl -s -S -f -o /dev/null -X POST "https://api.docudevs.ai/document/process/$JOB_GUID" \
    -H "Authorization: $API_KEY" \
    -H "Content-Type: application/json" \
    --data-binary @-

wait_for_job "$JOB_GUID"

curl -s -S -f "https://api.docudevs.ai/job/result/$JOB_GUID/json" \
  -H "Authorization: $API_KEY" | jq .
# docs:end
