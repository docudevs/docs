---
title: Page Range Processing
description: Process specific pages from multi-page documents for targeted extraction.
sidebar_position: 5
---

import Tabs from '@theme/Tabs';
import TabItem from '@theme/TabItem';

# Page Range Processing

Process specific pages from a document instead of the entire document.

The `pageRange` parameter allows you to target specific pages for extraction, which is useful for:

- **Multi-section documents**: Extract data from specific sections (e.g., pages 1-3 for identification, pages 8-10 for physical properties)
- **Large documents**: Focus on relevant pages without processing the entire document
- **Multi-pass processing**: Upload once, extract different data from different page ranges

## How It Works

1. **Upload** your document normally
2. **Specify `pageRange`** with 1-indexed page numbers
3. **Only specified pages** are sent to the AI for extraction

:::info Page Numbering
Pages are **1-indexed**, meaning the first page is page 1 (not 0).
:::

## Basic Usage

<Tabs
  defaultValue="python"
  values={[
    {label: 'Python SDK', value: 'python'},
    {label: 'cURL', value: 'curl'},
  ]}>
  <TabItem value="python">

```python
from docudevs.docudevs_client import DocuDevsClient
import os
import asyncio

client = DocuDevsClient(token=os.getenv('API_KEY'))

async def extract_first_page():
    with open("document.pdf", "rb") as f:
        document_data = f.read()

    # Process only page 1
    guid = await client.submit_and_process_document(
        document=document_data,
        document_mime_type="application/pdf",
        prompt="Extract the title and summary.",
        page_range=[1]
    )
    
    result = await client.wait_until_ready(guid, result_format="json")
    print(result)

asyncio.run(extract_first_page())
```

  </TabItem>
  <TabItem value="curl">

```bash
# Upload the document
GUID=$(curl -s -X POST https://api.docudevs.ai/document/upload \
  -H "Authorization: Bearer $API_KEY" \
  -F "document=@document.pdf" | jq -r '.guid')

# Process only page 1
curl -X POST "https://api.docudevs.ai/document/process/$GUID" \
  -H "Authorization: Bearer $API_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "prompt": "Extract the title and summary.",
    "mimeType": "application/pdf",
    "pageRange": [1]
  }'
```

  </TabItem>
</Tabs>

## Processing Multiple Specific Pages

You can specify any combination of pages:

```python
# Process pages 1, 3, and 5
guid = await client.submit_and_process_document(
    document=document_data,
    document_mime_type="application/pdf",
    prompt="Extract data from these specific pages.",
    page_range=[1, 3, 5]
)

# Process pages 8-10
guid = await client.submit_and_process_document(
    document=document_data,
    document_mime_type="application/pdf",
    prompt="Extract physical properties.",
    page_range=[8, 9, 10]
)
```

## Combined with Map-Reduce

Page range works with map-reduce processing. The specified pages are first filtered, then chunked and processed:

```python
# Document has 20 pages, but we only want pages 1-2, 5-6, and 10-11
# These 6 pages will be chunked into 3 chunks of 2 pages each
guid = await client.submit_and_process_document_map_reduce(
    document=document_data,
    document_mime_type="application/pdf",
    prompt="Extract all items from these pages.",
    schema=items_schema,
    pages_per_chunk=2,
    page_range=[1, 2, 5, 6, 10, 11]
)
```

:::note Processing Order
With `page_range=[1, 2, 5, 6, 10, 11]` and `pages_per_chunk=2`:

- **Chunk 1**: Pages 1-2 (original document pages 1-2)
- **Chunk 2**: Pages 5-6 (original document pages 5-6)  
- **Chunk 3**: Pages 10-11 (original document pages 10-11)

Each page is processed exactly once.
:::

## Use Case: Safety Data Sheets (SDS)

Safety Data Sheets have 16 standardized sections spread across many pages. You can extract specific sections using page ranges:

```python
# SDS document is 14 pages

# Extract Section 1: Identification (typically page 1)
identification_guid = await client.submit_and_process_document(
    document=sds_document,
    document_mime_type="application/pdf",
    prompt="Extract product name, manufacturer, and emergency contact.",
    schema=identification_schema,
    page_range=[1]
)

# Extract Section 9: Physical Properties (typically pages 8-9)
properties_guid = await client.submit_and_process_document(
    document=sds_document,
    document_mime_type="application/pdf",
    prompt="Extract appearance, odor, boiling point, and flash point.",
    schema=properties_schema,
    page_range=[8, 9]
)

# Extract Section 14-16: Transport & Regulatory (typically pages 12-14)
regulatory_guid = await client.submit_and_process_document(
    document=sds_document,
    document_mime_type="application/pdf",
    prompt="Extract transport classification and regulatory information.",
    schema=regulatory_schema,
    page_range=[12, 13, 14]
)
```

## Limitations

### OCR Compatibility

Page range requires page break markers from OCR, which are only available with `DEFAULT` or `PREMIUM` OCR modes.

:::warning LOW OCR Not Supported
`pageRange` cannot be used with `ocr=LOW` because the LOW OCR mode (MarkItDown) does not produce page break markers.
:::

```python
# This will raise an error
await client.submit_and_process_document(
    document=document_data,
    document_mime_type="application/pdf",
    page_range=[1, 2],
    ocr="LOW"  # ❌ Not supported
)
```

### Out-of-Range Pages

Pages that don't exist in the document are silently ignored:

```python
# Document has 10 pages
# Pages 11-15 will be ignored, only pages 8-10 are processed
guid = await client.submit_and_process_document(
    document=document_data,
    document_mime_type="application/pdf",
    prompt="Extract content.",
    page_range=[8, 9, 10, 11, 12, 13, 14, 15]
)
```

## API Reference

| Parameter | Type | Description |
|-----------|------|-------------|
| `pageRange` | `list[int]` | List of 1-indexed page numbers to process. If not provided, all pages are processed. |

The parameter is available in:

- `submit_and_process_document()`
- `submit_and_process_document_map_reduce()`
- `process_document()` (via UploadCommand)
- `process_document_map_reduce()`
- Direct API calls to `/document/process/{guid}`
