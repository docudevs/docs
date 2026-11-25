#!/usr/bin/env bash
set -euo pipefail

# docs:start
# The CLI has no option for parallel processing.
docudevs process-map-reduce contract.pdf \
  --prompt "Extract obligations by section." \
  --split-type markdown-header \
  --split-header-level 2
# docs:end
