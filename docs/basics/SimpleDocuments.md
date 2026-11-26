---
title: "Structured Data Extraction"
description: Extract structured data from documents using prompts, JSON schemas, or Pydantic models with the DocuDevs SDK.
sidebar_position: 1
---

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

<Example name="quick-start/process-document" />

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

<Example name="quick-start/custom-schema" />

The Python examples in this documentation describe their schemas with Pydantic models and pass them through `json_schema` (see [Type-Safe Extraction with Pydantic](#type-safe-extraction-with-pydantic-python)). A field with a default of `None` is optional; a field without a default is required. The cURL and CLI variants of the same example send the equivalent JSON Schema.

## Type-Safe Extraction with Pydantic (Python)

The Python SDK integrates seamlessly with **Pydantic**, allowing you to define your schema using Python classes. This gives you full type safety and IDE autocompletion. `json_schema` turns the model into a JSON Schema, and you validate the result back into the same model:

<Example name="extraction/extract-with-pydantic" />

## Advanced Configuration

You can combine extraction with other features like Barcode/QR scanning or specific OCR settings.

### Extracting QR Codes

<Example name="extraction/extract-qr-codes" />

## Reasoning Effort

Some models expose a configurable reasoning effort level. Set the optional `reasoningEffort` request field to override the model's default reasoning effort for every LLM call made while processing that job — including alternate-tier steps, pipeline nodes, and map-reduce chunk tasks.

`reasoningEffort` is a plain string, not a closed enum: DocuDevs forwards the value verbatim to the underlying model provider (for example, OpenAI-style values such as `low`, `medium`, or `high`), so newly supported effort levels do not require a DocuDevs release. An empty or whitespace-only value is rejected.

<Example name="extraction/set-reasoning-effort" />

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
