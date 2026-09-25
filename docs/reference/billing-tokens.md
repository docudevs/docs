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

The token packs available to your organization are configured in DocuDevs.

## Token consumption rules

- **LLM usage**: Each usage event with `event_type=llm` and `unit=token` is charged
  as blended tokens using the tier multiplier (`llm_tier`).
- **OCR usage**: Each usage event with `event_type=ocr` and `unit=page` is charged
  using the provider-specific tokens-per-page rate.
- **BYO providers**: If `provider_key` starts with `provider:` or
  `billing_exempt=true`, no tokens are debited.

Token rates are configurable so you can adjust pricing without changing Stripe
prices. New rates apply to future usage events.

### Per-tier billing for mixed-tier steps

In `STEPS` extraction mode, each step definition can set its own `llm` tier,
independent of the job's overall `llm` setting. When a job's steps use more
than one tier, usage is not blended into a single tier: each tier's usage is
aggregated and billed as its own usage event, tagged with that tier's
`llm_tier` dimension, and debited at that tier's own rate multiplier. A job
with some steps on `MINI` and others on `HIGH` is billed the `MINI` rate for
the `MINI` steps' tokens and the `HIGH` rate for the `HIGH` steps' tokens,
rather than the job's default tier rate for everything.

### Embedding usage

Embedding calls (document indexing, provider validation checks, and
knowledge-base search queries) are metered for visibility but do not
currently debit your token balance. This applies both to platform-managed
embeddings and to [bring-your-own embedding providers](/docs/administration/bring-your-own-embeddings).
Indexing and validation usage appears in usage reporting; query-time
embedding usage is not currently recorded there.

## Buying tokens and checking your balance

Buy token packs and check your current balance in the DocuDevs web app under
billing settings. The billing endpoints behind these pages are internal to the
web app and are not part of the public API reference; the Python SDK and CLI
have no billing helpers.

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
