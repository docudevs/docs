#!/usr/bin/env bash
set -euo pipefail

# docs:start
docudevs --token "$DOCUDEVS_API_KEY" cases update 123 \
  --name "Q4 2024 Vendor Invoices - Updated" \
  --description "Updated description with additional context"
# docs:end
