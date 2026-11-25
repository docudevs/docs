#!/usr/bin/env bash
set -euo pipefail

# docs:start
docudevs memo list 42 --status active
docudevs memo revisions 42 m_88

# Retiring adds a revision; earlier versions stay readable
docudevs memo retire 42 m_88
# docs:end
