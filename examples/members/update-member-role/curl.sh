#!/usr/bin/env bash
set -euo pipefail

# docs:start
# The member id comes from the list-members example (the "id" field).
MEMBER_ID=42

curl -s -S -f -X PATCH "https://api.docudevs.ai/members/$MEMBER_ID" \
  -H "Authorization: $API_KEY" \
  -H "Content-Type: application/json" \
  -d '{"role": "SUBMISSION_PORTAL_USER"}'
# docs:end
