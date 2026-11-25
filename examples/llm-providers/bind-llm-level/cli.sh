#!/usr/bin/env bash
set -euo pipefail

# docs:start
# List the current bindings of DEFAULT, NANO, MINI and HIGH
docudevs llm keys

# Bind the HIGH level to provider 7 (the provider "id"); omit --provider-id to clear a binding
docudevs llm bind HIGH --provider-id 7
# docs:end
