#!/usr/bin/env bash
set -euo pipefail

# docs:start
CASE_ID=42  # id of the case that holds the playbook documents

docudevs --token "$DOCUDEVS_API_KEY" knowledge-base contract-analysis get "$CASE_ID" --view draft
# docs:end
