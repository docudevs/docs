---
title: Billing and Credits
description: How DocuDevs bills processing in prepaid credits, what the rate card charges for OCR, parsing and LLM usage, and what bring-your-own models cost.
sidebar_position: 8
---

DocuDevs bills processing in credits. You buy credits in advance, and each
processing job deducts credits from your organization's balance. When the
balance is used up, new work is refused until you add credits.

## What a credit is

One credit is $0.00001, so $10 buys 1,000,000 credits. A credit is not an LLM
token: LLM usage is priced per 1,000 LLM tokens, and the price is expressed in
credits. Each charge is rounded up to a whole credit.

Credit packs are one-time purchases:

| Pack | Price | Credits |
| --- | ---: | ---: |
| Try It | $10 | 1,000,000 |
| Starter | $25 | 2,750,000 |
| Growth | $100 | 12,000,000 |
| Scale | $500 | 65,000,000 |

## The rate card

The rate card lists what each part of a job costs in credits:

- **OCR**: credits per page, by OCR provider.
- **Parsing without OCR**: Office and text documents pay a small platform fee
  per page. These documents have no fixed pages, so a page is counted as at
  most 3,000 characters of extracted text: a 30,000-character document is 10
  pages. Spreadsheets pay per row.
- **Your own OCR provider**: pages processed on an OCR provider you configured
  pay the same small per-page platform fee, not the OCR rate.
- **LLM**: credits per 1,000 input tokens and per 1,000 output tokens, for each
  level (Nano, Mini, Default, High). Each level runs a specific model, shown on
  the card. You are charged the rate of the model that actually ran.
- **Per-job fee**: a flat number of credits for every processing job.

The current card is on the DocuDevs pricing page, which also has a calculator
that estimates a job from page count and document type. The pricing page and
your charges come from the same rate card. Your charges can differ from the
page when your organization has custom OCR rates, or when a level runs a
different model than the card shows; you are charged the rate of the model that
ran.
Rates change when DocuDevs updates a model or its price; the card shows the
current values.

## Worked example

These numbers come from rate card version 2026-11-22.2. Check the pricing page
for current values.

A 3-page scanned document, processed with Default OCR and Default LLM
extraction, using 4,160 input and 130 output LLM tokens:

| Part | Calculation | Credits |
| --- | --- | ---: |
| OCR | 3 pages × 1,200 | 3,600 |
| LLM input | 4,160 tokens × 250 per 1,000 | 1,040 |
| LLM output | 130 tokens × 1,200 per 1,000 | 156 |
| Per-job fee | flat | 10 |
| Total | | 4,806 |

That is about $0.048.

A 12-page Word document (about 36,000 characters), processed with the text
parser and your own LLM, costs 12 pages × 10 + the per-job fee of 10 = 130
credits, about $0.0013.

Jobs with several LLM stages are charged per stage, and
each stage rounds up, so the total can be a few credits higher.

## Bring your own models

When a job uses your own LLM or embedding provider, DocuDevs does not charge for
that model's usage; you pay your provider directly. The per-job fee is still
charged, and so are the pages: OCR pages processed by DocuDevs at the OCR rate,
and parsed pages or pages on your own OCR provider at the per-page platform fee.
See [Bring your own LLM](/docs/administration/bring-your-own-llm) and
[Bring your own embeddings](/docs/administration/bring-your-own-embeddings).

Platform embeddings are not charged.

## Buying credits and checking your balance

Buy credit packs and check your balance in the DocuDevs web app under billing
settings. Billing is managed in the web app; the public API, SDKs and CLI have
no billing operations.

## Insufficient credits response

When the balance is used up, processing endpoints return `402`:

```json
{
  "status": 402,
  "code": "billing.insufficient_tokens",
  "message": "Credit balance is 0. Purchase more credits to continue.",
  "balance": 0,
  "checkout_url": "<checkout page URL>",
  "portal_url": "<billing portal URL>",
  "request_id": "...",
  "organization_id": 1
}
```

The `code` keeps its original name, `billing.insufficient_tokens`, for
compatibility; the balance it reports is in credits.
