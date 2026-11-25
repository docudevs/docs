---
title: Include Current Date
description: Pass the current date to the LLM so date-dependent extractions and calculations are accurate.
sidebar_position: 6
---

# Include Current Date

Provide the LLM with today's date so that date-relative calculations in your extractions produce correct results.

## Overview

LLMs do not inherently know the current date. If your extraction prompt or schema relies on date-dependent logic — for example computing the age of a vehicle, the remaining term of a contract, or how many days until an expiry date — the model may hallucinate or return inaccurate values.

Setting `includeCurrentDate: true` injects a short message containing today's date (ISO 8601 format) into the LLM conversation, giving the model the context it needs.

## When to Use

- **Age / duration calculations**: "How old is this vehicle?" or "Years since manufacture"
- **Relative date references**: "Is this contract still valid?" or "Days until expiry"
- **Depreciation or tax formulas**: Anything that compares a document date to "today"

If your extraction does not involve date arithmetic, you can leave the flag off (the default).

## Enabling Include Current Date

<Example name="extraction/include-current-date" />

The parameter is also available on map-reduce requests:

<Example name="extraction/include-current-date-map-reduce" />

## How It Works

When `includeCurrentDate` is `true`, the system adds a message to the LLM conversation before the document content:

```
Current date: 2025-07-17
```

The date is always in **ISO 8601** format (`YYYY-MM-DD`) and represents the server's current date at the time the extraction runs.

## Notes

- The flag defaults to `false` — existing requests are unaffected.
- Works with all extraction modes: `SIMPLE`, `STEPS`, and map-reduce.
- When used with map-reduce, every chunk receives the current date.
- The date is injected once per LLM call; it does not add significant token overhead.
