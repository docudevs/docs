#!/usr/bin/env bash
set -euo pipefail

# docs:start
CASE_ID=42  # id of the case that holds the playbook documents

docudevs --token "$DOCUDEVS_API_KEY" knowledge-base contract-analysis publish "$CASE_ID" \
  --draft-revision 2 \
  --acknowledge-warning UNCITED_HUMAN_AUTHORED
# docs:end
