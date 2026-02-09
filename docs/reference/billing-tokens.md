---
title: Billing Tokens
description: Token-based billing model with prepaid token packs, usage tracking, tier multipliers, and balance management.
sidebar_position: 8
---

# Billing Tokens

DocuDevs uses prepaid tokens for usage-based billing. Tokens are purchased as packs
in Stripe and consumed by LLM and OCR usage as jobs run.

## Token packs in Stripe

Create Stripe prices for token packs and set `metadata.tokens_granted` on each
price (integer). DocuDevs reads this metadata when creating checkout sessions
and when processing Stripe webhooks.

You can optionally restrict which token packs are exposed in DocuDevs by setting
an allowlist of price IDs or a single product ID (for self-hosted deployments).

## Token consumption rules

- **LLM usage**: Each usage event with `event_type=llm` and `unit=token` is charged
  as blended tokens using the tier multiplier (`llm_tier`).
- **OCR usage**: Each usage event with `event_type=ocr` and `unit=page` is charged
  using the provider-specific tokens-per-page rate.
- **BYO providers**: If `provider_key` starts with `provider:` or
  `billing_exempt=true`, no tokens are debited.

Token rates are configurable so you can adjust pricing without changing Stripe
prices. New rates apply to future usage events.

## API endpoints

- `GET /billing/prices`: List available token packs from Stripe.
- `POST /billing/checkout`: Create a Stripe checkout session for a token pack.
- `GET /billing/balance`: Fetch the current token balance.
- `POST /billing/webhook`: Stripe webhook for token credits.

## SDK usage (Python)

```python
from docudevs_client import DocuDevsClient

client = DocuDevsClient(token="your-api-key")

prices = await client.list_billing_prices()
session = await client.create_billing_checkout_session("price_123")
balance = await client.get_billing_balance()
```

## Insufficient token response

When tokens are exhausted, token-gated endpoints return `402`:

```json
{
  "status": 402,
  "code": "billing.insufficient_tokens",
  "message": "Token balance is 0. Purchase more tokens to continue.",
  "balance": 0,
  "checkout_url": "https://...",
  "portal_url": "https://...",
  "request_id": "...",
  "organization_id": 1
}
```
