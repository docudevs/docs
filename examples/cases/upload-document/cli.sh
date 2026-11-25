#!/usr/bin/env bash
set -euo pipefail

# docs:start
docudevs --token "$DOCUDEVS_API_KEY" cases upload-document 123 invoice_001.pdf
# docs:end
