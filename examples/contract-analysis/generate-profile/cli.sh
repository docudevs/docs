#!/usr/bin/env bash
set -euo pipefail

# docs:start
CASE_ID=42  # id of the case that holds the playbook documents

docudevs knowledge-base contract-analysis generate "$CASE_ID" \
  --our-party "Acme Corp" \
  --counterparty "Globex Ltd"
# docs:end
