#!/usr/bin/env bash
set -euo pipefail

# docs:start
# Grant uninvited identity-provider sign-ins the MEMBER role
curl -s -S -f -X PUT "https://api.docudevs.ai/members/join-policy" \
  -H "Authorization: $API_KEY" \
  -H "Content-Type: application/json" \
  -d '{"ssoDefaultRole": "MEMBER"}'

# Read the policy back
curl -s -S -f "https://api.docudevs.ai/members/join-policy" \
  -H "Authorization: $API_KEY"
# docs:end
