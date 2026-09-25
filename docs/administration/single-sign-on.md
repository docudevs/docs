---
title: Single Sign-On
description: Configure self-serve SSO — subscription, domain verification, and SAML/OIDC provider setup — from Settings > SSO.
sidebar_position: 2
---

# Single Sign-On

Organization admins configure single sign-on (SSO) themselves from **Settings > SSO** (`/app/settings/sso`). The page walks through three steps: subscribing to the SSO add-on, verifying the domains your organization signs in from, and configuring a SAML 2.0 or OIDC identity provider. The **SSO** link only appears in the settings sidebar for `ADMIN` users, and every backend call it makes requires the `ADMIN` role.

## Step 1 — Subscription

SSO is a paid add-on. The page shows the current subscription status and two actions:

- **Start trial / Update plan** — starts a Stripe checkout session for the SSO subscription.
- **Manage subscription** — opens a Stripe billing portal session (shown once a subscription exists).

Domain verification and identity-provider configuration (steps 2 and 3) only appear once the subscription status is `trialing`, `active`, or `past_due`.

## Step 2 — Domains

Add each domain your organization's users sign in from. For each domain:

1. Enter the domain and submit — DocuDevs generates a verification token.
2. Add a DNS `TXT` record with the value `docudevs-sso-verification=<token>` to prove ownership.
3. Click **Verify**. Once verified, the domain gets its own login URL (`/auth/login?ssoDomain=<domain>`) that routes sign-ins for that domain straight to your identity provider.

Unverified domains can be removed at any time.

## Step 3 — Identity provider

Configure exactly one provider per organization, either:

- **SAML 2.0** — a metadata URL or pasted metadata XML.
- **OIDC** — issuer URL, client ID, client secret, and scopes (defaults to `openid profile email`).

The form also has a **Default role** selector and an **Enforce SSO for verified domains** toggle, saved alongside the provider configuration:

- **Enforce SSO for verified domains** — once a domain is verified and enforcement is on, DocuDevs rejects sign-ins for that domain's users that didn't come through the configured identity provider (for example, a password-based sign-in for a user whose email domain is enforced is blocked). This does not create new accounts; it only restricts how existing members of an enforced domain can authenticate.
- **Default role** — stored with the provider configuration. Uninvited-user auto-provisioning is controlled separately by the **join policy** on **Settings > Members**, described below.

Saving configuration (**Save configuration**) validates and stores it in `DRAFT` status. **Activate SSO** pushes the configuration to the identity federation layer; the connection status becomes `SYNCED` on success or `ERROR` on failure (with a vendor-neutral error message — the underlying federation error is logged server-side, not returned to the client). Activating an OIDC provider for the first time requires the client secret; DocuDevs never echoes a previously-saved secret back to the UI (`oidcSecretConfigured` only indicates one is on file).

Activation requires at least one verified domain.

## Login discovery

`GET /sso/discovery?domain=<domain>` is the one unauthenticated endpoint in this feature — the DocuDevs login page calls it to check whether a domain has an active, discoverable SSO connection and to route the sign-in to the right provider. It returns 404 when no organization has that domain configured for SSO.

## Join policy for uninvited users

The **join policy**, on **Settings > Members** (not on this page), is a separate setting: the default role assigned to a user who signs in through an already-configured, single-tenant identity provider (for example, a dedicated Microsoft Entra ID deployment) without having been explicitly invited first. It has four choices — `ADMIN`, `MEMBER`, `SUBMISSION_PORTAL_USER`, or **Deny (invite only)**.

The current behavior:

- **Explicitly invited user signs in through the identity provider** — DocuDevs links that sign-in to the invited membership and activates the account automatically.
- **Uninvited user signs in and a default role is configured** — DocuDevs provisions that user with the configured role.
- **Uninvited user signs in and the policy is set to deny** — DocuDevs denies provisioning until an admin creates a membership explicitly.
- **User is suspended later** — standard suspended-account behavior still applies, regardless of how the account was created.

See [Members and Roles](./members-and-roles) for the full member lifecycle (invites, roles, suspension).

## REST reference

All endpoints below require an `ADMIN`-role principal (an org API key is always treated as `ADMIN`; a Supabase-session admin also qualifies) unless noted otherwise.

| Method | Path | Notes |
| --- | --- | --- |
| GET | `/sso` | Current subscription status, verified/pending domains, and provider configuration |
| POST | `/sso/checkout` | Start a Stripe checkout session for the SSO subscription |
| POST | `/sso/portal` | Open a Stripe billing portal session |
| POST | `/sso/domains` | Add a domain, `{domain}` — returns a verification token |
| POST | `/sso/domains/{id}/verify` | Verify a domain's DNS TXT record |
| DELETE | `/sso/domains/{id}` | Remove a domain |
| PUT | `/sso/config` | Save SAML/OIDC provider configuration (status becomes `DRAFT`) |
| POST | `/sso/sync` | Activate SSO — pushes the saved configuration to the identity federation layer |
| GET | `/sso/discovery?domain=` | Unauthenticated — used by the login page to resolve a domain to a provider |

```bash
curl -X PUT https://api.docudevs.ai/sso/config \
  -H "Authorization: $API_KEY" -H "Content-Type: application/json" \
  -d '{"providerType":"OIDC","oidcIssuerUrl":"https://idp.example.com","oidcClientId":"abc123",
       "oidcScopes":"openid profile email","enforceSsoForDomains":true}'

curl -X POST https://api.docudevs.ai/sso/sync \
  -H "Authorization: $API_KEY" -H "Content-Type: application/json" \
  -d '{"oidcClientSecret":"s3cret"}'

curl https://api.docudevs.ai/sso -H "Authorization: $API_KEY"
```
