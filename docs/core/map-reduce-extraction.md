---
title: Map-Reduce Extraction
description: Break long or repetitive documents into manageable windows while preserving header context.
sidebar_position: 4
---

import Tabs from '@theme/Tabs';
import TabItem from '@theme/TabItem';

# Map-Reduce Extraction

Process long documents (50+ pages) by breaking them into manageable chunks.

Map-Reduce is a powerful processing mode designed for long, repetitive, or multi-section documents. Instead of trying to fit the entire document into a single AI context window, DocuDevs splits the document into overlapping windows ("chunks"), extracts data from each chunk, and then aggregates the results.

## How It Works

1. **Map**: The document is split into chunks (e.g., 5 pages each).
2. **Process**: Each chunk is processed independently to extract data.
3. **Reduce**: The results are combined into a single, clean JSON output.

You always receive a consistent JSON payload:

```json
{
  "header": { ... optional ... },
  "records": [ ... rows ... ]
}
```

## When to Use Map-Reduce

- **Long Documents**: Annual reports, 100+ page contracts, large catalogs.
- **Repetitive Data**: Bank statements, medical records, logs.
- **Header + Rows**: Documents with a summary section (header) and pages of detailed rows.

## Configuration Options

| Option | Description | Default |
| --- | --- | --- |
| `pagesPerChunk` | Number of pages per extraction window. | `1` |
| `overlapPages` | Number of pages to overlap between chunks to catch data spanning pages. | `0` |
| `dedupKey` | JSON path to a unique field (e.g., `sku`, `date`) used to remove duplicates caused by overlap. | `null` |
| `header_options` | Configuration for extracting document-level metadata (header). | `null` |

## Example: Processing a Long Invoice

Imagine a 50-page invoice where the first page has the **Invoice Number** and **Date** (Header), and the remaining 49 pages contain **Line Items** (Rows).

<Tabs
  defaultValue="python"
  values={[
    {label: 'Python SDK', value: 'python'},
    {label: 'CLI', value: 'cli'},
    {label: 'cURL', value: 'curl'},
  ]}>
  <TabItem value="python">

```python
from docudevs.docudevs_client import DocuDevsClient
import os
import asyncio

client = DocuDevsClient(token=os.getenv('API_KEY'))

async def process_long_invoice():
    with open("large-invoice.pdf", "rb") as f:
        document_data = f.read()

    # Submit Map-Reduce Job
    job_id = await client.submit_and_process_document_map_reduce(
        document=document_data,
        document_mime_type="application/pdf",
        
        # Instructions for extracting rows (Line Items)
        prompt="Extract invoice line items (sku, description, quantity, unitPrice, total).",
        
        # Chunking Strategy
        pages_per_chunk=4,
        overlap_pages=1,
        dedup_key="lineItems.sku",  # Unique key to deduplicate rows
        
        # Header Extraction Strategy
        header_options={
            "page_limit": 1,  # Look for header only on page 1
            "include_in_rows": False, # Don't include page 1 in row processing
            "row_prompt_augmentation": "This is an invoice from Acme Corp." # Context for every chunk
        },
        header_schema='{"invoiceNumber":"string","issueDate":"string","billingAddress":"string"}'
    )

    print(f"Job submitted: {job_id}")
    
    # Wait for results
    result = await client.wait_until_ready(job_id, result_format="json")
    
    print("Header:", result.get("header"))
    print(f"Extracted {len(result.get('records', []))} line items.")

if __name__ == "__main__":
    asyncio.run(process_long_invoice())
```

  </TabItem>
  <TabItem value="cli">

```bash
# Process a long document using Map-Reduce
docudevs process-map-reduce large-invoice.pdf \
  --prompt "Extract line items" \
  --pages-per-chunk 4 \
  --overlap 1 \
  --dedup-key "sku" \
  --header-page-limit 1 \
  --header-schema '{"invoiceNumber": "string"}'
```

  </TabItem>
  <TabItem value="curl">

```bash
# Map-Reduce via API requires constructing the multipart request carefully.
# We recommend using the SDK for complex Map-Reduce jobs.
# However, you can use the standard process endpoint with specific parameters.

curl -X POST https://api.docudevs.ai/document/upload-files \
  -H "Authorization: Bearer $API_KEY" \
  -F "document=@large-invoice.pdf" \
  -F "mapReduceOptions='{
    \"pagesPerChunk\": 4,
    \"overlapPages\": 1,
    \"dedupKey\": \"sku\",
    \"headerOptions\": {\"pageLimit\": 1}
  }'"
```

  </TabItem>
</Tabs>

## Header Capture Strategy

Often, the first few pages contain "Header" information (Summary, Dates, Parties) that applies to the whole document.

- **`page_limit`**: How many pages at the start are considered "Header".
- **`header_schema`**: A specific JSON schema just for the header data.
- **`row_prompt_augmentation`**: Text injected into every subsequent chunk prompt. Useful for passing context (e.g., "This is Invoice #123") to help the AI understand isolated pages.

## Best Practices

1. **Always use a `dedupKey`**: When `overlapPages > 0`, the same row might appear in two chunks. The `dedupKey` tells DocuDevs how to identify and merge them.
2. **Start small**: Try `pagesPerChunk=4`. If the AI misses context, increase it. If it's too slow, decrease it.
3. **Monitor Progress**: Map-Reduce jobs take longer. Use the status endpoint to track progress.

```python
status = await client.status(job_id)
print(f"Progress: {status.parsed.mapReduceStatus.completedChunks} / {status.parsed.mapReduceStatus.totalChunks}")
```

## Troubleshooting

- **Missing Rows**: Check if rows are split across pages. Increase `overlapPages`.
- **Duplicate Rows**: Ensure your `dedupKey` is truly unique for each row.
- **Slow Processing**: Reduce `pagesPerChunk` to parallelize more effectively.
