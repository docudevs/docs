#!/usr/bin/env bash
set -euo pipefail

# docs:start
# CASE_ID is the id of an organization-wide case.
CASE_ID=3

curl -s -S -f -X POST "https://api.docudevs.ai/cases/$CASE_ID/embedding-reindex" \
  -H "Authorization: $API_KEY"
# docs:end
