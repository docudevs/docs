#!/usr/bin/env bash
set -euo pipefail

# docs:start
docudevs process invoice.pdf --ocr DEFAULT --extraction-mode OCR --barcodes --timeout 600
# docs:end
