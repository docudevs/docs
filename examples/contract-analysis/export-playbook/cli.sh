#!/usr/bin/env bash
set -euo pipefail

# docs:start
CASE_ID=42  # id of the case that holds the playbook documents

docudevs playbook export "$CASE_ID" --view draft > playbook.json

jq '.clauses | length' playbook.json
# docs:end
