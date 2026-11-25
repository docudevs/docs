#!/usr/bin/env bash
set -euo pipefail

# docs:start
CASE_ID=42  # id of the case that holds the playbook documents

docudevs knowledge-base contract-analysis get "$CASE_ID" --view draft > draft.json
jq '.profile' draft.json > profile.json
# edit profile.json, then save it back at the revision you read

docudevs knowledge-base contract-analysis update "$CASE_ID" \
  --draft-revision "$(jq '.draftRevision' draft.json)" \
  --profile-file profile.json \
  --reason "Reviewed clause wording"
# docs:end
