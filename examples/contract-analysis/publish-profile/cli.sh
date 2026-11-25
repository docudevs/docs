#!/usr/bin/env bash
set -euo pipefail

# docs:start
CASE_ID=42  # id of the case that holds the playbook documents

# List here the warning codes from the readiness or publish response that you accept, one --acknowledge-warning option each.
docudevs --token "$DOCUDEVS_API_KEY" knowledge-base contract-analysis publish "$CASE_ID" \
  --draft-revision 2
# docs:end
