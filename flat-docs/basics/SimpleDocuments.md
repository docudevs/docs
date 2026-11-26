---
title: "Structured Data Extraction"
description: Extract structured data from documents using prompts, JSON schemas, or Pydantic models with the DocuDevs SDK.
sidebar_position: 1
---
<!-- Generated from docs/basics/SimpleDocuments.md. Do not edit by hand. -->

# Structured Data Extraction

Turn any document into clean, structured JSON data.

DocuDevs allows you to extract specific fields from documents using natural language prompts or strict JSON schemas. This is the core feature of the platform, enabling you to process invoices, contracts, forms, and more with high precision.

## How It Works

1. **Upload** a document (PDF, Image, Word, etc.)
2. **Provide Instructions** (Prompt) or a **Schema**
3. **Receive JSON** data extracted from the document

## Basic Extraction (Prompt-based)

The simplest way to extract data is to describe what you want in plain English.

**Example:** Extracting data from a document with a prompt.

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

JOB_ID=$(curl -s -S -f -X POST "https://api.docudevs.ai/document/upload" \
  -H "Authorization: $API_KEY" \
  -F "document=@your-document.pdf" | jq -r '.guid')

curl -s -S -f -o /dev/null -X POST "https://api.docudevs.ai/document/process/$JOB_ID" \
  -H "Authorization: $API_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "mimeType": "application/pdf",
    "prompt": "Extract all key information from this document"
  }'
echo "Document submitted for processing. Job ID: $JOB_ID"

wait_for_job "$JOB_ID"
echo "Processing complete!"

curl -s -S -f "https://api.docudevs.ai/job/result/$JOB_ID/json" \
  -H "Authorization: $API_KEY" | jq .
```

**Python**

```python
import asyncio
import json
import os

from docudevs.docudevs_client import DocuDevsClient


async def main():
    client = DocuDevsClient(token=os.environ["API_KEY"])

    with open("your-document.pdf", "rb") as f:
        document_data = f.read()

    job_id = await client.submit_and_process_document(
        document=document_data,
        document_mime_type="application/pdf",
        prompt="Extract all key information from this document",
    )
    print(f"Document submitted for processing. Job ID: {job_id}")

    data = await client.wait_until_ready(job_id, result_format="json")
    print("Processing complete!")

    print("\nExtracted Data:")
    print(json.dumps(data, indent=2))


asyncio.run(main())
```

**Java**

```java
import ai.docudevs.client.DocuDevsClient;
import ai.docudevs.client.ProcessOptions;
import ai.docudevs.client.UploadRequest;
import ai.docudevs.client.WaitOptions;
import com.fasterxml.jackson.databind.JsonNode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

public class Example {
    public static void main(String[] args) throws Exception {
        DocuDevsClient client = DocuDevsClient.builder()
                .apiKey(System.getenv("API_KEY"))
                .baseUrl("https://api.docudevs.ai")
                .build();

        byte[] documentData = Files.readAllBytes(Path.of("your-document.pdf"));

        String jobId = client.submitAndProcessDocument(
                new UploadRequest("your-document.pdf", "application/pdf", documentData),
                ProcessOptions.builder()
                        .mimeType("application/pdf")
                        .prompt("Extract all key information from this document")
                        .build());
        System.out.println("Document submitted for processing. Job ID: " + jobId);

        JsonNode data = client.waitUntilReadyJson(
                jobId,
                WaitOptions.builder()
                        .timeout(Duration.ofSeconds(180))
                        .pollInterval(Duration.ofSeconds(5))
                        .build());
        System.out.println("Processing complete!");

        System.out.println("\nExtracted Data:");
        System.out.println(data.toPrettyString());
    }
}
```

**CLI**

```bash
docudevs process your-document.pdf \
  --prompt "Extract all key information from this document"
```

The cURL example uses [`jq`](https://jqlang.org/) to read values from responses.

For an invoice, the response looks like this:

```json
{
  "invoice_number": "INV-2024-001",
  "date": "2024-01-15",
  "total_amount": "1500.00 USD",
  "vendor_name": "Acme Corp"
}
```

## Structured Extraction (Schema-based)

For production use cases, you should use a **JSON Schema**. This ensures the API returns data in the exact format you need, with correct data types (numbers, dates, arrays).

**Why use a Schema?**

- **Consistency:** Always get the same JSON structure.
- **Type Safety:** Numbers are numbers, dates are standard strings.
- **Validation:** The AI knows exactly what fields are required.

### Example: Strict Invoice Schema

Define a schema for an invoice and pass it with the document. Use arrays of objects for repeating data such as line items, for example:

```json
"line_items": {
  "type": "array",
  "items": {
    "type": "object",
    "properties": {
      "description": {"type": "string"},
      "amount": {"type": "number"}
    }
  }
}
```

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

SCHEMA='{
  "type": "object",
  "properties": {
    "invoice_number": {"type": "string"},
    "date": {"type": "string", "format": "date"},
    "vendor": {"type": "string"},
    "total": {"type": "number"},
    "currency": {"type": "string"}
  },
  "required": ["invoice_number", "total"]
}'

JOB_ID=$(curl -s -S -f -X POST "https://api.docudevs.ai/document/upload" \
  -H "Authorization: $API_KEY" \
  -F "document=@your-document.pdf" | jq -r '.guid')

jq -n \
  --arg schema "$SCHEMA" \
  '{
    mimeType: "application/pdf",
    prompt: "Extract invoice data according to the schema",
    schema: $schema
  }' |
  curl -s -S -f -o /dev/null -X POST "https://api.docudevs.ai/document/process/$JOB_ID" \
    -H "Authorization: $API_KEY" \
    -H "Content-Type: application/json" \
    --data-binary @-

wait_for_job "$JOB_ID"

curl -s -S -f "https://api.docudevs.ai/job/result/$JOB_ID/json" \
  -H "Authorization: $API_KEY" | jq .
```

**Python**

```python
import datetime

from pydantic import BaseModel

from docudevs import json_schema

with open("your-document.pdf", "rb") as f:
    document_data = f.read()

class Invoice(BaseModel):
    invoice_number: str
    date: datetime.date | None = None
    vendor: str | None = None
    total: float
    currency: str | None = None

job_id = await client.submit_and_process_document(
    document=document_data,
    document_mime_type="application/pdf",
    prompt="Extract invoice data according to the schema",
    schema=json_schema(Invoice),
)

data = await client.wait_until_ready(job_id, result_format="json")
print(json.dumps(data, indent=2))
```

**Java**

```java
import ai.docudevs.client.ProcessOptions;
import ai.docudevs.client.Schema;
import ai.docudevs.client.UploadRequest;
import ai.docudevs.client.WaitOptions;
import com.fasterxml.jackson.databind.JsonNode;

byte[] documentData = Files.readAllBytes(Path.of("your-document.pdf"));

String schema = Schema.of(Map.of(
        "type", "object",
        "properties", Map.of(
                "invoice_number", Map.of("type", "string"),
                "date", Map.of("type", "string", "format", "date"),
                "vendor", Map.of("type", "string"),
                "total", Map.of("type", "number"),
                "currency", Map.of("type", "string")),
        "required", List.of("invoice_number", "total")));

String jobId = client.submitAndProcessDocument(
        new UploadRequest("your-document.pdf", "application/pdf", documentData),
        ProcessOptions.builder()
                .mimeType("application/pdf")
                .prompt("Extract invoice data according to the schema")
                .schema(schema)
                .build());

JsonNode data = client.waitUntilReadyJson(
        jobId,
        WaitOptions.builder()
                .timeout(Duration.ofSeconds(180))
                .pollInterval(Duration.ofSeconds(5))
                .build());
System.out.println(data.toPrettyString());
```

**CLI**

```bash
SCHEMA='{
  "type": "object",
  "properties": {
    "invoice_number": {"type": "string"},
    "date": {"type": "string", "format": "date"},
    "vendor": {"type": "string"},
    "total": {"type": "number"},
    "currency": {"type": "string"}
  },
  "required": ["invoice_number", "total"]
}'

docudevs process your-document.pdf \
  --prompt "Extract invoice data according to the schema" \
  --schema "$SCHEMA"
```

Most Python examples in this documentation describe their schemas with Pydantic models and pass them through `json_schema` (see [Type-Safe Extraction with Pydantic](#type-safe-extraction-with-pydantic-python)). A field with a default (usually `None`) is optional, and an optional field appears as nullable in the generated schema. A number you intend to export to Excel should be a required field (`float`, not `float | None`) so the column is written as numbers. The cURL and CLI variants of the same example send the equivalent JSON Schema.

## Type-Safe Extraction with Pydantic (Python)

The Python SDK integrates seamlessly with **Pydantic**, allowing you to define your schema using Python classes. This gives you full type safety and IDE autocompletion. `json_schema` turns the model into a JSON Schema, and you validate the result back into the same model:

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

# The JSON Schema for the Invoice model
SCHEMA='{
  "$defs": {
    "LineItem": {
      "properties": {
        "description": {"title": "Description", "type": "string"},
        "quantity": {"title": "Quantity", "type": "integer"},
        "price": {"title": "Price", "type": "number"}
      },
      "required": ["description", "quantity", "price"],
      "title": "LineItem",
      "type": "object"
    }
  },
  "properties": {
    "number": {"description": "The invoice number", "title": "Number", "type": "string"},
    "date": {"title": "Date", "type": "string"},
    "items": {"items": {"$ref": "#/$defs/LineItem"}, "title": "Items", "type": "array"},
    "total": {"title": "Total", "type": "number"}
  },
  "required": ["number", "date", "items", "total"],
  "title": "Invoice",
  "type": "object"
}'

JOB_ID=$(curl -s -S -f -X POST "https://api.docudevs.ai/document/upload" \
  -H "Authorization: $API_KEY" \
  -F "document=@invoice.pdf" | jq -r '.guid')

jq -n \
  --arg schema "$SCHEMA" \
  '{mimeType: "application/pdf", schema: $schema}' |
  curl -s -S -f -o /dev/null -X POST "https://api.docudevs.ai/document/process/$JOB_ID" \
    -H "Authorization: $API_KEY" \
    -H "Content-Type: application/json" \
    --data-binary @-

wait_for_job "$JOB_ID"

curl -s -S -f "https://api.docudevs.ai/job/result/$JOB_ID/json" \
  -H "Authorization: $API_KEY" | jq .
```

**Python**

```python
from pydantic import BaseModel, Field

from docudevs import json_schema

# 1. Define your data model
class LineItem(BaseModel):
    description: str
    quantity: int
    price: float

class Invoice(BaseModel):
    number: str = Field(description="The invoice number")
    date: str
    items: list[LineItem]
    total: float

with open("invoice.pdf", "rb") as f:
    document_data = f.read()

# 2. Generate the schema from the model and process the document
job_id = await client.submit_and_process_document(
    document=document_data,
    document_mime_type="application/pdf",
    schema=json_schema(Invoice),
)

# 3. Validate the result back into the model
result_dict = await client.wait_until_ready(job_id, result_format="json")
invoice = Invoice(**result_dict)

print(f"Invoice {invoice.number} total: {invoice.total}")
```

## Advanced Configuration

You can combine extraction with other features like Barcode/QR scanning or specific OCR settings.

### Extracting QR Codes

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

JOB_ID=$(curl -s -S -f -X POST "https://api.docudevs.ai/document/upload" \
  -H "Authorization: $API_KEY" \
  -F "document=@invoice.pdf" | jq -r '.guid')

curl -s -S -f -o /dev/null -X POST "https://api.docudevs.ai/document/process/$JOB_ID" \
  -H "Authorization: $API_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "mimeType": "application/pdf",
    "prompt": "Extract QR codes",
    "barcodes": true
  }'

wait_for_job "$JOB_ID"

curl -s -S -f "https://api.docudevs.ai/job/result/$JOB_ID/json" \
  -H "Authorization: $API_KEY" | jq .
```

**Python**

```python
with open("invoice.pdf", "rb") as f:
    document_data = f.read()

job_id = await client.submit_and_process_document(
    document=document_data,
    document_mime_type="application/pdf",
    prompt="Extract QR codes",
    barcodes=True,  # Enable barcode scanning
)

result = await client.wait_until_ready(job_id, result_format="json")
print(json.dumps(result, indent=2))
```

**Java**

```java
import ai.docudevs.client.DocuDevsClient;
import ai.docudevs.client.ProcessOptions;
import ai.docudevs.client.UploadRequest;
import ai.docudevs.client.WaitOptions;
import com.fasterxml.jackson.databind.JsonNode;
import java.nio.file.Files;
import java.nio.file.Path;

public class Example {
    public static void main(String[] args) throws Exception {
        DocuDevsClient client = DocuDevsClient.builder()
                .apiKey(System.getenv("API_KEY"))
                .build();

        byte[] fileBytes = Files.readAllBytes(Path.of("invoice.pdf"));
        UploadRequest upload = new UploadRequest("invoice.pdf", "application/pdf", fileBytes);

        String guid = client.submitAndProcessDocument(
                upload,
                ProcessOptions.builder()
                        .prompt("Extract QR codes")
                        .mimeType("application/pdf")
                        .barcodes(true)
                        .build());

        JsonNode result = client.waitUntilReadyJson(guid, WaitOptions.builder().build());
        System.out.println(result);
    }
}
```

**CLI**

```bash
docudevs process invoice.pdf --barcodes --prompt "Extract QR codes"
```

## Reasoning Effort

Some models expose a configurable reasoning effort level. Set the optional `reasoningEffort` request field to override the model's default reasoning effort for every LLM call made while processing that job — including alternate-tier steps, pipeline nodes, and map-reduce chunk tasks.

`reasoningEffort` is a plain string, not a closed enum: DocuDevs forwards the value verbatim to the underlying model provider (for example, OpenAI-style values such as `low`, `medium`, or `high`), so newly supported effort levels do not require a DocuDevs release. An empty or whitespace-only value is rejected.

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

JOB_ID=$(curl -s -S -f -X POST "https://api.docudevs.ai/document/upload" \
  -H "Authorization: $API_KEY" \
  -F "document=@invoice.pdf" | jq -r '.guid')

curl -s -S -f -o /dev/null -X POST "https://api.docudevs.ai/document/process/$JOB_ID" \
  -H "Authorization: $API_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "mimeType": "application/pdf",
    "prompt": "Extract the invoice number, date, total amount, and vendor name.",
    "reasoningEffort": "low"
  }'

wait_for_job "$JOB_ID"

curl -s -S -f "https://api.docudevs.ai/job/result/$JOB_ID/json" \
  -H "Authorization: $API_KEY" | jq .
```

**Python**

```python
with open("invoice.pdf", "rb") as f:
    document_data = f.read()

job_id = await client.submit_and_process_document(
    document=document_data,
    document_mime_type="application/pdf",
    prompt="Extract the invoice number, date, total amount, and vendor name.",
    reasoning_effort="low",  # forwarded to the model provider
)

result = await client.wait_until_ready(job_id, result_format="json")
print(json.dumps(result, indent=2))
```

**Java**

```java
import ai.docudevs.client.DocuDevsClient;
import ai.docudevs.client.ProcessOptions;
import ai.docudevs.client.UploadRequest;
import ai.docudevs.client.WaitOptions;
import com.fasterxml.jackson.databind.JsonNode;
import java.nio.file.Files;
import java.nio.file.Path;

public class Example {
    public static void main(String[] args) throws Exception {
        DocuDevsClient client = DocuDevsClient.builder()
                .apiKey(System.getenv("API_KEY"))
                .build();

        byte[] fileBytes = Files.readAllBytes(Path.of("invoice.pdf"));
        UploadRequest upload = new UploadRequest("invoice.pdf", "application/pdf", fileBytes);

        String guid = client.submitAndProcessDocument(
                upload,
                ProcessOptions.builder()
                        .mimeType("application/pdf")
                        .prompt("Extract the invoice number, date, total amount, and vendor name.")
                        .reasoningEffort("low")
                        .build());

        JsonNode result = client.waitUntilReadyJson(guid, WaitOptions.builder().build());
        System.out.println(result.toPrettyString());
    }
}
```

The CLI has no `--reasoning-effort` flag; use the Python SDK, the Java SDK, or the raw HTTP API.

## Upload Responses

The upload and synchronous processing endpoints accept documents for the authenticated organization. Upload endpoints return a job `guid`; use the job status and result endpoints to retrieve asynchronous output.

| Endpoint | Success response |
| --- | --- |
| `POST /document/upload-files` | `UploadResponse` JSON with the job `guid` and `status` |
| `POST /document/upload-files/sync` | JSON string containing the extracted text |
| `POST /document/ocr/sync` | JSON string containing OCR output |
| `POST /document/generate-schema` | `UploadResponse` JSON with the job `guid` and `status` |

### Busy and conflict responses

`POST /document/analyze`, `POST /submission-portals/{id}/submissions` and the deprecated `POST /cases/{caseId}/upload` can return:

- **409 Conflict**: the request conflicts with an active review run for the document. For a submission portal, the request can also conflict with a batch start.
- **503 Service Unavailable**: the service is busy with the document. Retry the request after a short delay.

`POST /cases/{caseId}/documents` returns **503** in the same way. Its **409** means the case is a records case, which does not accept documents; import records instead.

Error responses carry the API's JSON error body. The Python SDK's detailed calls expose the HTTP status and raw body; the Java SDK reports non-success responses as `DocuDevsApiException` (`getStatusCode()`, `getResponseBody()`).

The batch endpoints `POST /document/batch/{guid}/upload`, `/process`, `/process/with-configuration/{configurationName}` and `/schedule` return **404 Not Found** when the batch job (or the requested configuration) does not exist, **409 Conflict** when the batch state or a review run prevents the request, and **503 Service Unavailable** when the service is busy with the job; retry a 503 after a short delay.

### Child Operation Submissions

The following endpoints create or reuse a child job under an existing job:

| Endpoint | Success response |
| --- | --- |
| `POST /operation` | `SubmitOperationResponse` JSON |
| `POST /operation/{parentJobId}/generative-task` | `SubmitOperationResponse` JSON |
| `POST /job/{parentGuid}/pdf-acroform/apply` | `SubmitOperationResponse` JSON |
| `POST /compare` | `CompareSubmitResponse` JSON with HTTP 202 |
| `POST /compare/{compareGuid}/exports` | `CompareExportSubmitResponse` JSON with HTTP 202 |
| `POST /contract-analysis/runs` | `ContractAnalysisRunResponse` JSON |

These endpoints can return:

- **409 Conflict**: the child job is bound to a review run. `POST /operation` returns a JSON `message` and also uses 409 for an operation identity conflict or a contract-analysis publication that must be revalidated (`revalidation_required`). For `POST /contract-analysis/runs`, 409 means the run identity conflicts with the requested execution configuration.
- **503 Service Unavailable**: the service is busy. Retry after a short delay. For a contract-analysis run, retry with the same idempotency key to recover the recorded run identity and status; a replay does not schedule another attempt. If the replay reports `ERROR` and you want a new attempt, submit with a new idempotency key.

## Next Steps

- **[Map-Reduce Extraction](../core/map-reduce-extraction.md)**: Handle very large documents (50+ pages).
- **[Batch Processing](../core/batch-processing.md)**: Process thousands of documents efficiently.
- **[Named Configurations](../configuration/Configuration.md)**: Save your schemas and prompts for reuse.
