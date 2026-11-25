#!/usr/bin/env bash
set -euo pipefail

# docs:start
docudevs records search 42 "Soft sports sock" --kind records --top-k 10 --subtree 4.12 --attribute brand=Foo
# docs:end
