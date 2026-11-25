#!/usr/bin/env bash
set -euo pipefail

# docs:start
# List the current OCR key bindings
docudevs ocr keys

# Bind the DEFAULT key to provider 7 (the provider "id"); omit --provider-id to clear the binding
docudevs ocr bind DEFAULT --provider-id 7
# docs:end
