#!/usr/bin/env bash
set -euo pipefail

# docs:start
# The member id comes from the list-members example (the "id" field).
MEMBER_ID=42

# Answers 204 with no body.
curl -s -S -f -X DELETE "https://api.docudevs.ai/members/$MEMBER_ID" \
  -H "Authorization: $API_KEY"
# docs:end
