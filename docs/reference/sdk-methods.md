---
title: SDK Methods
sidebar_position: 4
---

# SDK Method Reference

Complete reference for the DocuDevs Python SDK (`docudevs-sdk`).

## Client Initialization

### DocuDevsClient

Initialize the client with your API key.

```python
from docudevs.docudevs_client import DocuDevsClient
import os

client = DocuDevsClient(
    api_url="https://api.docudevs.ai",  # Optional, defaults to production
    token=os.getenv("DOCUDEVS_API_KEY") # Required
)
```

## Document Processing

### submit_and_process_document

Upload and process a document for structured data extraction.

```python
job_guid = await client.submit_and_process_document(
    document=document_bytes,
    document_mime_type="application/pdf",
    prompt="Extract invoice data",
    schema={...},  # Optional JSON schema
    ocr="PREMIUM", # Optional: DEFAULT, PREMIUM, LOW
    llm="PREMIUM"  # Optional: DEFAULT, PREMIUM
)
```

### submit_and_process_document_with_configuration

Process a document using a saved configuration.

```python
job_guid = await client.submit_and_process_document_with_configuration(
    document=document_bytes,
    document_mime_type="application/pdf",
    configuration_name="invoice-config"
)
```

### submit_and_ocr_document

Process a document with OCR only (no structured extraction).

```python
job_guid = await client.submit_and_ocr_document(
    document=document_bytes,
    document_mime_type="application/pdf",
    ocr="PREMIUM",
    ocr_format="markdown", # markdown, plain, jsonl (for Excel)
    describe_figures=True
)
```

### wait_until_ready

Wait for a job to complete and retrieve the result.

```python
result = await client.wait_until_ready(
    guid=job_guid,
    timeout=180,
    poll_interval=5.0,
    result_format="json" # json, csv, excel, or None (legacy object)
)
```

## Batch Processing

### submit_and_process_batch

Upload and process multiple documents as a batch.

```python
batch_guid = await client.submit_and_process_batch(
    documents=[file1_bytes, file2_bytes],
    document_mime_type="application/pdf",
    prompt="Extract data",
    max_concurrency=5
)
```

## Configurations

### save_configuration

Save a named configuration.

```python
from docudevs.models import UploadCommand

config = UploadCommand(
    prompt="Extract invoice data",
    ocr="PREMIUM"
)
await client.save_configuration("invoice-config", config)
```

### list_configurations

List all saved configurations.

```python
configs = await client.list_configurations()
```

### get_configuration

Get details of a specific configuration.

```python
config = await client.get_configuration("invoice-config")
```

### delete_configuration

Delete a configuration.

```python
await client.delete_configuration("invoice-config")
```

## Templates

### upload_template

Upload a new document template.

```python
with open("form.pdf", "rb") as f:
    await client.upload_template(
        name="form-template",
        document=f,
        mime_type="application/pdf"
    )
```

### list_templates

List all available templates.

```python
templates = await client.list_templates()
```

### metadata

Get metadata (fields) for a template.

```python
meta = await client.metadata("form-template")
```

### fill

Fill a template with data.

```python
from docudevs.models import TemplateFillRequest

request = TemplateFillRequest(fields={"name": "John"})
response = await client.fill("form-template", request)
```

### delete_template

Delete a template.

```python
await client.delete_template("form-template")
```

## Cases

### create_case

Create a new case (collection of documents).

```python
from docudevs.models import CreateCaseBody

case = await client.create_case(
    body=CreateCaseBody(name="Q1 Invoices")
)
```

### list_cases

List all cases.

```python
cases = await client.list_cases()
```

### upload_case_document

Upload a document to a case.

```python
from docudevs.models import UploadCaseDocumentBody
from docudevs.types import File

await client.upload_case_document(
    case_id=123,
    body=UploadCaseDocumentBody(
        document=File(payload=data, file_name="doc.pdf")
    )
)
```

## Operations

### submit_and_wait_for_error_analysis

Run error analysis on a completed job.

```python
analysis = await client.submit_and_wait_for_error_analysis(job_guid)
```

### submit_and_wait_for_generative_task

Run a generative AI task on a completed job.

```python
task = await client.submit_and_wait_for_generative_task(
    parent_job_id=job_guid,
    prompt="Summarize this document",
    model="DEFAULT"
)
# Result is in task.result (JSON string)
```

### submit_and_wait_for_operation_with_parameters

Run an operation with custom parameters.

```python
result = await client.submit_and_wait_for_operation_with_parameters(
    job_guid=job_guid,
    operation_type="error-analysis",
    llm_type="HIGH",
    custom_parameters={"focus": "dates"}
)
```

## Map-Reduce Helpers

### submit_and_process_document_map_reduce

Process large documents using map-reduce strategy.

```python
job_guid = await client.submit_and_process_document_map_reduce(
    document=doc_bytes,
    document_mime_type="application/pdf",
    prompt="Extract line items",
    pages_per_chunk=5,
    overlap_pages=1,
    dedup_key="sku"
)
```

## LLM Tracing

### get_trace

Get the LLM trace for a completed job (only available if `trace=True` was set).

```python
trace = await client.get_trace(job_guid)
if trace:
    print(f"Total tokens: {trace['total_tokens']}")
    print(f"LLM calls: {trace['total_llm_calls']}")
    for event in trace['events']:
        print(f"  {event['type']}: {event['name']}")
```

### get_image

Get a page thumbnail image from a processed job.

```python
image_bytes = await client.get_image(job_guid, page_index=0)
if image_bytes:
    with open("page_0.png", "wb") as f:
        f.write(image_bytes)
```

### Enabling Tracing

Pass `trace=True` to any processing method:

```python
job_guid = await client.submit_and_process_document(
    document=doc_bytes,
    document_mime_type="application/pdf",
    prompt="Extract data",
    trace=True  # Enable LLM tracing
)
```

## Error Handling

The SDK raises exceptions for API errors.

```python
try:
    await client.get_configuration("non-existent")
except Exception as e:
    print(f"Error: {e}")
```
