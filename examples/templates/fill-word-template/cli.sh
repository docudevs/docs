#!/usr/bin/env bash
set -euo pipefail

# docs:start
cat > report_data.json <<'EOF'
{
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
}
EOF

docudevs fill sales_report report_data.json --output filled_report.docx
# docs:end
