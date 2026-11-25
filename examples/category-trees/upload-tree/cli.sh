#!/usr/bin/env bash
set -euo pipefail

# docs:start
CASE_ID=42  # id of a records case

cat > tree.json <<'JSON'
{
  "name": "Catalogue 2026-10",
  "nodes": [
    {"code": "4", "parent": null, "name": "Clothing"},
    {"code": "4.12", "parent": "4", "name": "Socks and tights"},
    {"code": "4.12.3", "parent": "4.12", "name": "Socks", "description": "Socks worn inside shoes"},
    {"code": "9.90.1", "parent": "9.90", "name": "OLD Socks", "status": "retired", "successor": "4.12.3"}
  ]
}
JSON

docudevs trees upload "$CASE_ID" tree.json
# docs:end
