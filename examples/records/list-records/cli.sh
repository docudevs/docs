#!/usr/bin/env bash
set -euo pipefail

# docs:start
CASE_ID=42  # id of a records case

docudevs records list "$CASE_ID" --subtree 4.12 --attribute brand=Foo --all
# docs:end
