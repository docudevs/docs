---
title: Batch Processing
description: Process multiple documents in a single batch with configurable concurrency, shared prompts, and aggregated results.
sidebar_position: 5
---
<!-- Generated from docs/core/batch-processing.md. Do not edit by hand. -->

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

The example creates a batch with a concurrency limit of three, uploads three invoices, starts processing with a prompt and a schema, waits for the batch to finish and prints the result for each document. The results list follows the upload order; a document that failed or has no stored result is `null` in the list (see [Best Practices](#best-practices) and [Result Formats](https://docs.docudevs.ai/docs/core/result-formats#batch-results)).

**cURL**

```bash
wait_for_job() {
  local deadline=$((SECONDS + 600))
  while [ "$SECONDS" -lt "$deadline" ]; do
    STATUS=$(curl -s -S -f "https://api.docudevs.ai/job/status/$1" \
      -H "Authorization: $API_KEY" | jq -r '.status')
    case "$STATUS" in
      COMPLETED) return 0 ;;
      PARTIAL) echo "Job $1 ended with status PARTIAL" >&2; return 0 ;;
      ERROR|FAILED|TIMEOUT) echo "Job $1 ended with status $STATUS" >&2; return 1 ;;
    esac
    sleep 5
  done
  echo "Job $1 did not finish within 600 seconds" >&2
  return 1
}

BATCH_GUID=$(curl -s -S -f -X POST "https://api.docudevs.ai/document/batch" \
  -H "Authorization: $API_KEY" \
  -H "Content-Type: application/json" \
  -d '{"maxConcurrency": 3}' | jq -r '.jobGuid')

for FILENAME in invoice1.pdf invoice2.pdf invoice3.pdf; do
  curl -s -S -f -o /dev/null -X POST "https://api.docudevs.ai/document/batch/$BATCH_GUID/upload" \
    -H "Authorization: $API_KEY" \
    -F "document=@$FILENAME"
done

SCHEMA='{
  "type": "object",
  "properties": {
    "invoice_number": {"type": "string"},
    "total": {"type": "number"}
  },
  "required": ["total"]
}'

jq -n \
  --arg schema "$SCHEMA" \
  '{
    mimeType: "application/pdf",
    prompt: "Extract invoice data",
    schema: $schema
  }' |
  curl -s -S -f -o /dev/null -X POST "https://api.docudevs.ai/document/batch/$BATCH_GUID/process" \
    -H "Authorization: $API_KEY" \
    -H "Content-Type: application/json" \
    --data-binary @-

wait_for_job "$BATCH_GUID"

# A document that failed or has no stored result is null in this list
curl -s -S -f "https://api.docudevs.ai/job/result/$BATCH_GUID/json" \
  -H "Authorization: $API_KEY" | jq .
```

**Python**

```python
from pydantic import BaseModel

from docudevs import json_schema

batch_guid = await client.create_batch(max_concurrency=3)

for filename in ["invoice1.pdf", "invoice2.pdf", "invoice3.pdf"]:
    with open(filename, "rb") as f:
        await client.upload_batch_document(
            batch_guid=batch_guid,
            document=f.read(),
            mime_type="application/pdf",
            file_name=filename,
        )

class Invoice(BaseModel):
    invoice_number: str | None = None
    total: float

await client.process_batch(
    batch_guid=batch_guid,
    mime_type="application/pdf",
    prompt="Extract invoice data",
    schema=json_schema(Invoice),
)

results = await client.wait_until_ready(batch_guid, timeout=600, result_format="json")
for i, result in enumerate(results):
    if result is None:
        print(f"Document {i}: no result (it failed or has no stored result)")
    else:
        print(f"Document {i}: {result}")
```

**Java**

```java
import ai.docudevs.client.ProcessOptions;
import ai.docudevs.client.Schema;
import ai.docudevs.client.UploadRequest;
import ai.docudevs.client.WaitOptions;
import com.fasterxml.jackson.databind.JsonNode;

String batchGuid = client.createBatch(3);

for (String filename : List.of("invoice1.pdf", "invoice2.pdf", "invoice3.pdf")) {
    byte[] documentData = Files.readAllBytes(Path.of(filename));
    client.uploadBatchDocument(
            batchGuid,
            new UploadRequest(filename, "application/pdf", documentData));
}

String schema = Schema.of(Map.of(
        "type", "object",
        "properties", Map.of(
                "invoice_number", Map.of("type", "string"),
                "total", Map.of("type", "number")),
        "required", List.of("total")));

client.processBatch(
        batchGuid,
        ProcessOptions.builder()
                .mimeType("application/pdf")
                .prompt("Extract invoice data")
                .schema(schema)
                .build());

JsonNode results = client.waitUntilReadyJson(
        batchGuid,
        WaitOptions.builder()
                .timeout(Duration.ofSeconds(600))
                .pollInterval(Duration.ofSeconds(5))
                .build());
for (int i = 0; i < results.size(); i++) {
    if (results.get(i).isNull()) {
        System.out.println("Document " + i + ": no result (it failed or has no stored result)");
    } else {
        System.out.println("Document " + i + ": " + results.get(i));
    }
}
```

**CLI**

```bash
SCHEMA='{
  "type": "object",
  "properties": {
    "invoice_number": {"type": "string"},
    "total": {"type": "number"}
  },
  "required": ["total"]
}'

docudevs batch process invoice1.pdf invoice2.pdf invoice3.pdf \
  --prompt "Extract invoice data" \
  --schema "$SCHEMA" \
  --mime-type application/pdf \
  --max-concurrency 3 \
  --format json \
  --output results.json
```

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
- **Error Handling**: Batch jobs continue even if individual documents fail. A failed or missing document is `null` in the results list, and an empty row in CSV or Excel. Check each entry for `null`; the `status` and `error` of each document are in `documents` of `GET /job/result/{guid}`. See [Result Formats](https://docs.docudevs.ai/docs/core/result-formats#batch-results).
