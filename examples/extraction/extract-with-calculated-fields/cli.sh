#!/usr/bin/env bash
set -euo pipefail

# docs:start
cat > schema-with-calculated-fields.json <<'JSON'
{
  "type": "object",
  "properties": {
    "net_amount": {"type": "number"},
    "vat_rate": {"type": "number"},
    "gross_amount": {
      "type": "number",
      "x-docudevs": {
        "fieldType": "calculated",
        "dependsOn": ["net_amount", "vat_rate"],
        "formula": {
          "lang": "excel",
          "expression": "ROUND({net_amount} * (1 + {vat_rate}), 2)"
        }
      }
    }
  }
}
JSON

docudevs process invoice.pdf \
  --prompt "Extract net amount and vat rate as numbers." \
  --schema-file schema-with-calculated-fields.json
# docs:end
