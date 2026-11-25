#!/usr/bin/env bash
set -euo pipefail

# docs:start
docudevs --token "$DOCUDEVS_API_KEY" knowledge-base contract-analysis get 42 --view published
# docs:end
