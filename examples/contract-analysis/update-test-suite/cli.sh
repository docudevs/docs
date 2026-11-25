#!/usr/bin/env bash
set -euo pipefail

# docs:start
CASE_ID=42  # id of the case that holds the playbook documents

docudevs knowledge-base contract-analysis suite-get "$CASE_ID" > suite.json
jq '.cases' suite.json > cases.json
# add or edit cases in cases.json, then save the next revision

docudevs knowledge-base contract-analysis suite-update "$CASE_ID" \
  --revision "$(jq '.revision' suite.json)" \
  --cases cases.json
# docs:end
