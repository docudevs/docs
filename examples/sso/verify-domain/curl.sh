#!/usr/bin/env bash
set -euo pipefail

# docs:start
# The domain id comes from the add-domain example (the "id" field).
DOMAIN_ID=7

# Answers 400 until the DNS TXT record is published.
curl -s -S -f -X POST "https://api.docudevs.ai/sso/domains/$DOMAIN_ID/verify" \
  -H "Authorization: $API_KEY" \
  | jq '{id, domain, verifiedAt}'
# docs:end
