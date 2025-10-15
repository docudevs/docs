---
title: Map-Reduce Extraction
description: Break long or repetitive documents into manageable windows while preserving header context.
sidebar_position: 4
---

Map-Reduce is an optional processing mode that helps you extract structured data from long, repetitive, or multi-section documents. DocuDevs splits the document into windows, extracts the relevant data, and then merges the results into a single response. You always receive a consistent JSON payload:

```json
{
  "header": { ... optional ... },
  "records": [ ... rows ... ]
}
```

- `records` contains the aggregated row data (always present).
- `header` is included only when you request a header pass.

No additional orchestration is required—every SDK helper and API endpoint returns the structure above.

## When to use Map-Reduce

Choose Map-Reduce when:

- The document is very long (annual statements, large catalogues, multi-section reports).
- Important data is repeated across pages (product listings, recurring ledger entries, meter readings).
- Header facts (invoice metadata, report summary, measurement certificate) should guide row extraction.

Stay with the default single-pass mode when the document is short or does not repeat content.

## Key options

| Option | Purpose |
| --- | --- |
| `pagesPerChunk` | Number of pages (or OCR segments) per extraction window. |
| `overlapPages` | Pages carried over from the previous window to catch rows that span boundaries. |
| `dedupKey` | Field used to remove duplicates when overlap is enabled. |
| `header_options` | Toggle the header pass, set page limit, and configure row prompt augmentation. |
| `header_schema` | (SDK) JSON schema for the header response, separate from the row schema. |
| `header_prompt` | (SDK) Prompt override for the header pass. |

### Header capture

When `header_options` is enabled:

- The first `page_limit` pages (default 1) are processed separately.
- You can provide `header_schema` so header data is structured independently of the row schema.
- `header_prompt` (SDK) lets you tailor the header pass without changing the main prompt.
- `row_prompt_augmentation` injects the extracted header facts into each subsequent row window.

Whether you request a header or not, the service always returns `{ "header": {...?}, "records": [...] }`.

### Choosing chunk size and overlap

1. Start with `pagesPerChunk=4` and `overlapPages=1`.
2. Increase `pagesPerChunk` if chunks lose context (e.g., multi-page rows).
3. Decrease `pagesPerChunk` if latency or token usage is high.
4. Set `overlapPages=0` when rows never cross page boundaries.

## Quick examples

Below are two end-to-end examples using the Python SDK. Both return the canonical `{header, records}` structure.

### Invoice with header + line items

```python
from docudevs.docudevs_client import DocuDevsClient
import asyncio

async def run_invoice_example() -> None:
    client = DocuDevsClient(token="YOUR_API_KEY")
    with open("sample-invoice.pdf", "rb") as fh:
        payload = fh.read()

    job_id = await client.submit_and_process_document_map_reduce(
        document=payload,
        document_mime_type="application/pdf",
        prompt=(
            "Extract invoice header information (invoiceNumber, issueDate, billing address) and "
            "invoice line items (sku, description, quantity, unitPrice, total)."
        ),
        pages_per_chunk=4,
        overlap_pages=1,
        dedup_key="lineItems.sku",
        header_options={
            "page_limit": 2,
            "include_in_rows": False,
            "row_prompt_augmentation": "Invoice header context"
        },
        header_schema='{"invoiceNumber":"string","issueDate":"string","billingAddress":"string"}'
    )

    result = await client.wait_until_ready(job_id, result_format="json")
    print("Invoice header:", result.get("header"))
    print("First line item:", result["records"][0] if result["records"] else None)

asyncio.run(run_invoice_example())
```

### Industrial equipment report

```python
from docudevs.docudevs_client import DocuDevsClient
import asyncio

async def run_equipment_report() -> None:
    client = DocuDevsClient(token="YOUR_API_KEY")
    with open("machine-report.pdf", "rb") as fh:
        payload = fh.read()

    header_schema = '{"reportNumber":"string","equipment":"string","technician":"string"}'
    rows_schema = '{"type":"array","items":{"type":"object","properties":{"metric":{"type":"string"},"measured":{"type":"string"},"nominal":{"type":"string"},"tolerancePlus":{"type":"string"},"toleranceMinus":{"type":"string"},"deviation":{"type":"string"}},"required":["metric","measured"]}}'

    job_id = await client.submit_and_process_document_map_reduce(
        document=payload,
        document_mime_type="application/pdf",
        prompt="Extract equipment measurements (metric, measured, nominal, tolerance, deviation).",
        schema=rows_schema,
        pages_per_chunk=2,
        overlap_pages=0,
        dedup_key="metric",
        header_options={
            "page_limit": 1,
            "include_in_rows": False,
            "row_prompt_augmentation": "Equipment report header context"
        },
        header_schema=header_schema
    )

    result = await client.wait_until_ready(job_id, result_format="json")
    print("Report header:", result.get("header"))
    print("First measurement:", result["records"][0] if result["records"] else None)

asyncio.run(run_equipment_report())
```

## Practical tips

- **Pick a dedup key** whenever you use overlap. Use a stable identifier that exists on every row (e.g., SKU, metric name).
- **Keep header schema small.** Only include the fields you plan to use later (e.g., for prompt augmentation or downstream joins).
- **Monitor progress.** `GET /job/status/{guid}` reports `totalFragments`, `completedFragments`, and `mapReduceHeaderCaptured` so you can display user-friendly progress.
- **Timeouts.** Long windows take longer to process; consider smaller chunks if jobs approach your timeout budget.

## Frequently asked questions

**Do I need to manage chunking myself?**
No. You only provide the parameters; the service handles splitting, retries, and aggregation.

**Can I combine Map-Reduce with Steps or Templates?**
Not yet. Map-Reduce is currently available for the “simple” extraction mode.

**What if the document has no header?**
Leave `header_options` and `header_schema` unset. The response will simply contain `{ "records": [...] }`.

**How do I know the response is complete?**
Poll `wait_until_ready(..., result_format="json")` (or the REST status endpoint). Once you receive a 200 with `status=COMPLETED`, the `records` array is final.

Ready to adopt Map-Reduce? Try the examples above or call the `/document/process` endpoint with your own document payload.
