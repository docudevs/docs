#!/usr/bin/env bash
set -euo pipefail

# docs:start
docudevs --token "$DOCUDEVS_API_KEY" cases get 123
# docs:end
