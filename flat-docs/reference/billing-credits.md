---
title: Billing and Credits
description: How DocuDevs bills processing in prepaid credits, what the rate card charges for OCR, parsing and LLM usage, and what bring-your-own models cost.
sidebar_position: 8
---
<!-- Generated from docs/reference/billing-credits.md. Do not edit by hand. -->

DocuDevs bills processing in credits. You buy credits in advance, and each
processing job deducts credits from your organization's balance. When the
balance is used up, new work is refused until you add credits.

## What a credit is

A credit is not an LLM token: LLM usage is priced per 1,000 LLM tokens, and the
price is expressed in credits. Each charge is rounded up to a whole credit.

Credits are sold in packs as one-time purchases. The packs and their current
prices are on the DocuDevs pricing page and in the web app under billing
settings.

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

## How a job is charged

The cost of a job is the sum of its parts, each taken from the current rate
card:

| Part | Calculation |
| --- | --- |
| OCR | pages × the OCR rate per page |
| Parsing without OCR | pages × the per-page platform fee |
| LLM input | input tokens ÷ 1,000 × the input rate of the model that ran |
| LLM output | output tokens ÷ 1,000 × the output rate of the model that ran |
| Per-job fee | flat |

A scanned document processed with OCR and LLM extraction pays the OCR, LLM and
per-job parts. A Word document processed with the text parser and your own LLM
pays only the per-page platform fee and the per-job fee.

Jobs with several LLM stages are charged per stage, and
each stage rounds up, so the total can be a few credits higher.

## Bring your own models

When a job uses your own LLM or embedding provider, DocuDevs does not charge for
that model's usage; you pay your provider directly. The per-job fee is still
charged, and so are the pages: OCR pages processed by DocuDevs at the OCR rate,
and parsed pages or pages on your own OCR provider at the per-page platform fee.
See [Bring your own LLM](https://docs.docudevs.ai/docs/administration/bring-your-own-llm) and
[Bring your own embeddings](https://docs.docudevs.ai/docs/administration/bring-your-own-embeddings).

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
