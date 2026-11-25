#!/usr/bin/env bash
set -euo pipefail

# docs:start
docudevs memo add 42 instruction "Fiction books are filed by genre when the description names one." --scope subtree --code 7.31

# Nothing changes in classification until the instruction is approved; use the id the add command printed
docudevs memo approve 42 m_88
# docs:end
