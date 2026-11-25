#!/usr/bin/env bash
set -euo pipefail

# docs:start
CASE_ID=42  # id of a records case with a configured dataset

# A dry run: it changes nothing and prints the report
docudevs records sync "$CASE_ID" snapshot.ndjson > dry-run.json
jq '.syncSummary | {counts, guard}' dry-run.json

# After reviewing the report, apply the same file for real
docudevs records sync "$CASE_ID" snapshot.ndjson --confirm "$(jq -r '.importId' dry-run.json)"
# docs:end
