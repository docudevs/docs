#!/usr/bin/env bash
set -euo pipefail

# docs:start
docudevs process invoice.pdf --barcodes --prompt "Extract QR codes"
# docs:end
