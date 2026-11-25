#!/usr/bin/env bash
set -euo pipefail

# docs:start
docudevs --token "$DOCUDEVS_API_KEY" knowledge-base contract-analysis publish 42 \
  --draft-revision 2 \
  --acknowledge-warning UNCITED_HUMAN_AUTHORED
# docs:end
