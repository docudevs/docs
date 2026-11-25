#!/usr/bin/env bash
set -euo pipefail

# docs:start
CASE_ID=42  # id of the case that holds the playbook documents

# playbook.json holds a playbook in the playbook format; without --draft-revision
# the import replaces the current draft
docudevs playbook import "$CASE_ID" playbook.json
# docs:end
