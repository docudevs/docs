#!/usr/bin/env bash
set -euo pipefail

# docs:start
docudevs --token "$DOCUDEVS_API_KEY" process your-document.pdf \
  --prompt "Extract all key information from this document"
# docs:end
