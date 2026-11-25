#!/usr/bin/env bash
set -euo pipefail

# docs:start
docudevs batch process invoice1.pdf invoice2.pdf \
  --configuration invoice-config \
  --lookup-file vendors-2024.csv
# docs:end
