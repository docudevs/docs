#!/usr/bin/env bash
set -euo pipefail

# docs:start
docudevs --token "$DOCUDEVS_API_KEY" knowledge-base contract-analysis generate 42 \
  --our-party "Acme Corp" \
  --counterparty "Globex Ltd"
# docs:end
