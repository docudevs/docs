#!/usr/bin/env bash
set -euo pipefail

# docs:start
# OIDC_CLIENT_SECRET holds the client secret issued by your identity provider.
# Save the provider configuration (status DRAFT)
curl -s -S -f -X PUT "https://api.docudevs.ai/sso/config" \
  -H "Authorization: $API_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "providerType": "OIDC",
    "oidcIssuerUrl": "https://idp.example.com",
    "oidcClientId": "docudevs-client",
    "oidcScopes": "openid profile email",
    "enforceSsoForDomains": true
  }'

# Activate SSO; the first activation of an OIDC provider needs the client secret
curl -s -S -f -X POST "https://api.docudevs.ai/sso/sync" \
  -H "Authorization: $API_KEY" \
  -H "Content-Type: application/json" \
  -d "$(jq -n --arg secret "$OIDC_CLIENT_SECRET" '{oidcClientSecret: $secret}')"

# Read the subscription, domains and provider status
curl -s -S -f "https://api.docudevs.ai/sso" \
  -H "Authorization: $API_KEY"
# docs:end
