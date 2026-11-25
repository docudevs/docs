#!/usr/bin/env bash
set -euo pipefail

# docs:start
CASE_ID=42  # id of a records case with an active category tree

cat > rows.json <<'JSON'
[
  {
    "key": "6410000999999",
    "text": {"title": "Running socks 3-pack", "description": "Cotton blend"}
  }
]
JSON
echo '{"target": "category", "llmLevel": "MINI", "targetDepth": [4, 5]}' > config.json

docudevs classify start "$CASE_ID" --rows-file rows.json --config-file config.json --no-wait
# docs:end
