#!/usr/bin/env bash
set -euo pipefail

# docs:start
docudevs configuration lookup-file upload invoice-config vendors.csv

# The CLI has no command to download a lookup file.

docudevs configuration lookup-file delete invoice-config
# docs:end
