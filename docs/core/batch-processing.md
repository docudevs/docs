---
title: Batch Processing
sidebar_position: 5
---

Coordinate the processing of multiple homogeneous documents under a single parent job. Batches let you upload several files, run one configuration across all of them, and retrieve an ordered list of per-document results.

## When To Use

Choose batches when:

- You have dozens or hundreds of similar documents that share the same prompt, schema, or template
- Consolidated progress tracking and a single job identifier simplifies downstream automation
- You want to reprocess a collection without re-uploading each file

Prefer single-document processing when you only need to submit a few files or each document requires different configuration.

## Lifecycle Overview

1. **Create** an empty batch job to reserve a GUID
2. **Upload** documents one at a time (each upload receives an index)
3. **Process** the batch by providing the extraction configuration
4. **Monitor** progress and completion through the standard status endpoint
5. **Fetch** results as a list aligned with the upload order

## Core Concepts

| Concept | Description |
|---------|-------------|
| isBatch | Flag on the parent job that identifies batch processing |
| totalDocuments | Count of documents currently attached to the batch |
| completedDocuments | Number of documents that finished successfully |
| failedDocuments | Number of documents that completed with errors |
| maxConcurrency | Upper bound of documents processed in parallel |
| jobMode | `STANDARD`, `MAP_REDUCE`, or other operation modes applied to each document |

## API Workflow

### 1. Create a Batch

```bash
curl -X POST "https://api.docudevs.ai/document/batch" \
  -H "Authorization: Bearer $API_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "mimeType": "application/pdf"
  }'
```

### 2. Upload Documents

```bash
curl -X POST "https://api.docudevs.ai/document/batch/${BATCH_GUID}/upload" \
  -H "Authorization: Bearer $API_KEY" \
  -F "document=@reports/q1.pdf"
```

Each upload response includes the assigned index and updated totals:
The `schema` field should contain a JSON schema string—use the same structure you pass to single-document extractions.

```json
{
  "jobGuid": "9f1f7ef0-5e0c-4d33-9f03-4c499df0c4d2",
  "index": 0,
  "totalDocuments": 1
}
```

Repeat uploads until every document is attached.

### 3. Process the Batch

```bash
curl -X POST "https://api.docudevs.ai/document/batch/${BATCH_GUID}/process" \
  -H "Authorization: Bearer $API_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "prompt": "Extract the statement date, customer name, and running balance.",
    "schema": "{\\n  \\\"statements\\\": [{\\n    \\\"statementDate\\\": \\\"date\\\",\\n    \\\"customerName\\\": \\\"string\\\",\\n    \\\"runningBalance\\\": \\\"number\\\"\\n  }]\\n}",
    "maxConcurrency": 4
  }'
```

You can rerun processing with updated configuration. Reprocessing clears previous results but keeps uploaded documents.

### 4. Check Status

```bash
curl -X GET "https://api.docudevs.ai/job/status/${BATCH_GUID}" \
  -H "Authorization: Bearer $API_KEY"
```

Sample response while processing:

```json
{
  "guid": "9f1f7ef0-5e0c-4d33-9f03-4c499df0c4d2",
  "status": "PROCESSING",
  "isBatch": true,
  "totalDocuments": 10,
  "completedDocuments": 6,
  "failedDocuments": 1,
  "maxConcurrency": 4
}
```

### 5. Retrieve Results

```bash
curl -X GET "https://api.docudevs.ai/job/result/${BATCH_GUID}/json" \
  -H "Authorization: Bearer $API_KEY"
```

Results return as an ordered array. Indices align with the upload order.

```json
[
  {
    "statements": [
      {
        "statementDate": "2024-01-31",
        "customerName": "Northern Supplies LLC",
        "runningBalance": 18234.12
      }
    ]
  },
  null,
  "ERROR: Document failed due to unreadable content"
]
```

## Python SDK Example

```python
import asyncio
import json
import os

from docudevs.docudevs_client import DocuDevsClient


async def run_batch() -> None:
    client = DocuDevsClient(token=os.environ["DOCUDEVS_API_KEY"])

    batch_guid = await client.create_batch(max_concurrency=3)

    for path in ["reports/january.pdf", "reports/february.pdf", "reports/march.pdf"]:
        with open(path, "rb") as handle:
            await client.upload_batch_document(
                batch_guid=batch_guid,
                document=handle.read(),
                mime_type="application/pdf",
                file_name=os.path.basename(path),
            )

    schema = json.dumps(
        {
            "statements": [
                {
                    "statementDate": "date",
                    "customerName": "string",
                    "runningBalance": "number",
                }
            ]
        }
    )

    await client.process_batch(
        batch_guid=batch_guid,
        mime_type="application/pdf",
        prompt="Extract the statement date, customer name, and running balance.",
        schema=schema,
    )

    results = await client.wait_until_ready(
        batch_guid,
        poll_interval=2,
        result_format="json",
    )

    for index, document_result in enumerate(results):
        if document_result is None:
            print(f"Document {index + 1}: pending or failed")
        elif isinstance(document_result, str):
            print(f"Document {index + 1} error: {document_result}")
        else:
            statements = document_result["statements"]
            print(f"Document {index + 1} statements: {statements}")


asyncio.run(run_batch())
```

## Result Semantics

- The response array length always matches `totalDocuments`
- Completed documents return structured data using the same schema as single-document jobs
- Failed documents return an error marker string (future revisions may include structured error objects)
- Pending documents return `null`
- Reprocessing overwrites all per-document results while keeping the array order stable

## Progress & Concurrency

| Scenario | Recommendation |
|----------|----------------|
| Many small PDFs | Set `maxConcurrency` between 3 and 8 to balance throughput and cost |
| Large files or GPU-heavy chains | Lower `maxConcurrency` to avoid long-running overloads |
| Mixed file sizes | Start with a moderate concurrency (3–4) and adjust after observing queue behavior |
| Partial failures | Continue processing other documents, then fix and reprocess only the failed ones |

Workers schedule new documents as they finish existing ones, ensuring no more than `maxConcurrency` documents are active at the same time.

## Best Practices

- Validate that every document can share the same prompt or schema before batching
- Keep a manifest on your side that maps indices to original filenames for easier reconciliation
- Call processing only after all uploads succeed; the API rejects processing when `totalDocuments` is zero
- Use `wait_until_ready(..., result_format="json")` so the SDK returns a ready-to-use Python list instead of raw strings
- On reprocess, clear or archive previous consumer outputs to avoid mixing historical results with fresh data

## Troubleshooting

| Symptom | Possible Cause | Fix |
|---------|----------------|-----|
| Process request returns 400 | No documents uploaded or invalid configuration | Upload documents first and ensure prompt/schema is provided |
| Status stuck in PROCESSING | Long-running documents or low concurrency | Increase `maxConcurrency` or check worker logs for problematic files |
| Result array smaller than expected | Client misread response body | Ensure you are parsing JSON as a list and not truncating on first `null` |
| Frequent failures on specific index | Corrupted file or unsupported format | Download the per-document error artifact, fix source file, and reprocess |

## See Also

- [Simple Document Extraction](../basics/SimpleDocuments.md)
- [Map-Reduce Extraction](./map-reduce-extraction.md)
- [SDK Methods Reference](../reference/sdk-methods.md)
