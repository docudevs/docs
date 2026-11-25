#!/usr/bin/env bash
set -euo pipefail

# docs:start
# The command waits for the job and prints the dataset.
# The CLI has no command for the plan, source-map or summary artifacts.
docudevs process submission.xlsx \
  --extraction-mode WORKBOOK \
  --prompt "Find related tables. Preserve source values, units, row grain, and source references."
# docs:end
