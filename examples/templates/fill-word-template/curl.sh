#!/usr/bin/env bash
set -euo pipefail

# docs:start
curl -s -S -f -X POST "https://api.docudevs.ai/template/fill/sales_report" \
  -H "Authorization: $API_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "fields": {
      "reportTitle": "Quarterly Sales",
      "date": "2024-04-01",
      "items": [
        {"product": "Widget A", "sales": 100, "revenue": 5000},
        {"product": "Widget B", "sales": 200, "revenue": 8000},
        {"product": "Widget C", "sales": 50, "revenue": 2500}
      ],
      "summary": {
        "totalRevenue": 15500,
        "growth": "15%"
      }
    }
  }' \
  --output filled_report.docx
echo "Saved filled_report.docx"
# docs:end
