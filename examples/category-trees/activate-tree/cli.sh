#!/usr/bin/env bash
set -euo pipefail

# docs:start
CASE_ID=42  # id of a records case
VERSION_ID=7  # treeVersionId returned when the tree was uploaded

echo '{"9.90.1": "4.12.3"}' > successors.json

docudevs trees activate "$CASE_ID" "$VERSION_ID" --successor-file successors.json --wait
# docs:end
