#!/usr/bin/env bash
set -euo pipefail

# docs:start
# This command only prints the action and exits 0 even when it ends "failed"; read the status in the output
docudevs agent confirm 9001
# docs:end
