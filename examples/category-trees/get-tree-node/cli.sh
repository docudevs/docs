#!/usr/bin/env bash
set -euo pipefail

# docs:start
CASE_ID=42  # id of a records case
VERSION_ID=7  # treeVersionId returned when the tree was uploaded

docudevs trees node "$CASE_ID" "$VERSION_ID" 4.12.3
# docs:end
