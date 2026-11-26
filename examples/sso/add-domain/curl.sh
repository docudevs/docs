#!/usr/bin/env bash
set -euo pipefail

# docs:start
# Answers 200 with the domain's id, verificationToken and, once verified, verifiedAt.
curl -s -S -f -X POST "https://api.docudevs.ai/sso/domains" \
  -H "Authorization: $API_KEY" \
  -H "Content-Type: application/json" \
  -d '{"domain": "example.com"}' \
  | jq '{id, domain, txtRecord: ("docudevs-sso-verification=" + .verificationToken)}'
# docs:end
