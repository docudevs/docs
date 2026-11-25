#!/usr/bin/env bash
set -euo pipefail

# docs:start
docudevs --token "$DOCUDEVS_API_KEY" cases delete-document 123 doc-uuid-here
# docs:end
