#!/usr/bin/env bash
set -euo pipefail

# docs:start
# --ocr: DEFAULT, PREMIUM or AUTO. --format: markdown or plain
docudevs ocr-only handwritten.pdf --ocr PREMIUM --format plain
# docs:end
