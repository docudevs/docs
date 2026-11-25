#!/usr/bin/env bash
set -euo pipefail

# docs:start
# invoice-schema.json holds the schema from the generation step, after review
docudevs process new-invoice.pdf --schema-file invoice-schema.json
# docs:end
