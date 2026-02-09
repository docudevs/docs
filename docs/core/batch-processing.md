---
title: Batch Processing
description: Process multiple documents in a single batch with configurable concurrency, shared prompts, and aggregated results.
sidebar_position: 5
---

import Tabs from '@theme/Tabs';
import TabItem from '@theme/TabItem';

# Batch Processing

Coordinate the processing of multiple homogeneous documents under a single parent job.

Batches let you upload several files, run one configuration across all of them, and retrieve an ordered list of per-document results.

## When To Use

Choose batches when:

- You have dozens or hundreds of similar documents that share the same prompt, schema, or template.
- You want consolidated progress tracking and a single job identifier.
- You want to reprocess a collection without re-uploading each file.

## Lifecycle Overview

1. **Create** an empty batch job to reserve a GUID.
2. **Upload** documents one at a time (each upload receives an index).
3. **Process** the batch by providing the extraction configuration.
4. **Monitor** progress and completion.
5. **Fetch** results as a list aligned with the upload order.

## Step-by-Step Guide

<Tabs
  defaultValue="python"
  values={[
    {label: 'Python SDK', value: 'python'},
    {label: 'cURL', value: 'curl'},
  ]}>
  <TabItem value="python">

```python
import asyncio
import json
import os
from docudevs.docudevs_client import DocuDevsClient

async def run_batch():
    client = DocuDevsClient(token=os.environ["API_KEY"])

    # 1. Create Batch
    batch_guid = await client.create_batch(max_concurrency=3)
    print(f"Created batch: {batch_guid}")

    # 2. Upload Documents
    files = ["invoice_jan.pdf", "invoice_feb.pdf", "invoice_mar.pdf"]
    for path in files:
        with open(path, "rb") as f:
            await client.upload_batch_document(
                batch_guid=batch_guid,
                document=f.read(),
                mime_type="application/pdf",
                file_name=os.path.basename(path),
            )
    print(f"Uploaded {len(files)} documents.")

    # 3. Process Batch
    schema = {
        "statements": [{
            "date": "date",
            "customer": "string",
            "total": "number"
        }]
    }
    
    await client.process_batch(
        batch_guid=batch_guid,
        mime_type="application/pdf",
        prompt="Extract statement details.",
        schema=json.dumps(schema),
    )
    print("Processing started...")

    # 4. Wait for Results
    results = await client.wait_until_ready(
        batch_guid,
        poll_interval=2,
        result_format="json",
    )

    # 5. Handle Results
    for i, result in enumerate(results):
        if isinstance(result, str):
            print(f"Doc {i} Error: {result}")
        elif result is None:
            print(f"Doc {i} Pending/Failed")
        else:
            print(f"Doc {i} Data: {result}")

if __name__ == "__main__":
    asyncio.run(run_batch())
```

  </TabItem>
  <TabItem value="curl">

### 1. Create a Batch

```bash
curl -X POST "https://api.docudevs.ai/document/batch" \
  -H "Authorization: Bearer $API_KEY" \
  -H "Content-Type: application/json" \
  -d '{ "mimeType": "application/pdf" }'
```

### 2. Upload Documents

Repeat for each file:

```bash
curl -X POST "https://api.docudevs.ai/document/batch/${BATCH_GUID}/upload" \
  -H "Authorization: Bearer $API_KEY" \
  -F "document=@invoice_jan.pdf"
```

### 3. Process the Batch

```bash
curl -X POST "https://api.docudevs.ai/document/batch/${BATCH_GUID}/process" \
  -H "Authorization: Bearer $API_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "prompt": "Extract statement details.",
    "schema": "...",
    "maxConcurrency": 4
  }'
```

### 4. Check Status

```bash
curl -X GET "https://api.docudevs.ai/job/status/${BATCH_GUID}" \
  -H "Authorization: Bearer $API_KEY"
```

### 5. Retrieve Results

```bash
curl -X GET "https://api.docudevs.ai/job/result/${BATCH_GUID}/json" \
  -H "Authorization: Bearer $API_KEY"
```

  </TabItem>
</Tabs>

## Core Concepts

| Concept | Description |
|---------|-------------|
| `isBatch` | Flag on the parent job that identifies batch processing. |
| `totalDocuments` | Count of documents currently attached to the batch. |
| `completedDocuments` | Number of documents that finished successfully. |
| `failedDocuments` | Number of documents that completed with errors. |
| `maxConcurrency` | Upper bound of documents processed in parallel. |

## Best Practices

- **Concurrency**: Set `maxConcurrency` between 3 and 8 for optimal throughput. Lower it for very large files.
- **Validation**: Ensure all documents in a batch are of the same type (e.g., all invoices) so they share the same schema.
- **Error Handling**: Batch jobs continue even if individual documents fail. Always check the results list for error strings.
