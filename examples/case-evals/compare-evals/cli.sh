#!/usr/bin/env bash
set -euo pipefail

# docs:start
docudevs eval compare 42 301 302 --baseline-variant baseline --candidate-variant with-rule

# Scoring is asynchronous: read the result with the evalId the compare command printed
docudevs eval status 42 303 --wait
# docs:end
