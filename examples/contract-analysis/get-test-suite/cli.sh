#!/usr/bin/env bash
set -euo pipefail

# docs:start
docudevs --token "$DOCUDEVS_API_KEY" knowledge-base contract-analysis suite-get 42
# docs:end
