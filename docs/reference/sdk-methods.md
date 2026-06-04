---
title: SDK Methods
description: Complete Python SDK reference for DocuDevs including document processing, pipeline mode, batch operations, cases, templates, configurations, OCR, map-reduce, and agent chat methods.
sidebar_position: 4
---

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

## Schema Helpers

Convenience functions for building JSON Schema strings from Pydantic models or dicts.

### json_schema

Convert a Pydantic model or dict to a JSON Schema string. Use for single-object extraction.

```python
from docudevs import json_schema
from pydantic import BaseModel, Field

class Invoice(BaseModel):
    number: str = Field(description="Invoice number")
    total: float

# From a Pydantic model
schema = json_schema(Invoice)

# From a dict
schema = json_schema({"type": "object", "properties": {"name": {"type": "string"}}})
```

### array_schema

Wrap a Pydantic model or dict as an array schema. Use for map-reduce extraction where each chunk produces multiple items.

```python
from docudevs import array_schema
from pydantic import BaseModel

class LineItem(BaseModel):
    sku: str
    description: str
    quantity: int

# Produces {"type": "array", "items": <LineItem schema>}
schema = array_schema(LineItem)

# From a dict
schema = array_schema({"type": "object", "properties": {"sku": {"type": "string"}}})
```

Both helpers accept:
- **Pydantic model classes** — calls `model_json_schema()` automatically
- **Dicts** — used as-is

## Document Processing

### submit_and_process_document

Upload and process a document for structured data extraction. Use `extract_figures=True` to store figure images and metadata. For fillable PDFs, `acro_form_metadata=True` stores AcroForm metadata as a separate job artifact that you can fetch later with `get_acroform_metadata(...)`.

```python
job_guid = await client.submit_and_process_document(
    document=document_bytes,
    document_mime_type="application/pdf",
    prompt="Extract invoice data",
    schema={...},  # Optional JSON schema
    ocr="PREMIUM", # Optional: DEFAULT, PREMIUM, LOW
    llm="HIGH",  # Optional: DEFAULT, MINI, HIGH
    extract_figures=True,
    acro_form_metadata=True,
    source_locations=True,
    source_location_granularity="block",
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

Process a document with OCR only (no structured extraction). Use `extract_figures=True` to store figure images and metadata.

```python
job_guid = await client.submit_and_ocr_document(
    document=document_bytes,
    document_mime_type="application/pdf",
    ocr="PREMIUM",
    ocr_format="markdown", # markdown, plain, jsonl (for Excel)
    describe_figures=True,
    extract_figures=True
)
```

### analyze_document

Analyze document structure and return a job GUID.

```python
job_guid = await client.analyze_document(
    document=document_bytes,
    document_mime_type="application/pdf",
    ocr="PREMIUM"
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

## Pipeline Processing

Pipeline mode uploads one document, runs one shared OCR pass, and executes a graph of nodes. Use the SDK builder so dependencies, source paths, and final outputs refer to previous nodes directly.

### process_pipeline_document

Upload and process a document with a pipeline builder definition.

```python
from docudevs import P, Pipeline

pipeline = Pipeline().ocr(mode="AUTO")
extract_data = pipeline.extract(
    "extract_data",
    source=P.ocr.content,
    prompt="Extract the requested fields.",
    schema={
        "type": "object",
        "properties": {"name": {"type": "string"}},
    },
)
pipeline.final(
    "final_result",
    depends_on=[extract_data],
    output=extract_data.result,
)

job_guid = await client.process_pipeline_document(
    document=document_bytes,
    document_mime_type="application/pdf",
    pipeline=pipeline,
    ocr="AUTO",
    trace=True
)

result = await client.wait_until_ready(job_guid, result_format="json")
nodes = await client.get_pipeline_nodes(job_guid)
```

### process_uploaded_pipeline_document

Run pipeline processing on a document that was already uploaded.

```python
await client.process_uploaded_pipeline_document(
    guid=uploaded_guid,
    pipeline=pipeline,
    mime_type="application/pdf",
    ocr="AUTO"
)
```

### save_pipeline_configuration and get_pipeline_configuration

Save and retrieve named pipeline configurations.

```python
await client.save_pipeline_configuration(
    "router-pipeline",
    pipeline=pipeline,
    mime_type="application/pdf",
    ocr="AUTO"
)

saved_pipeline = await client.get_pipeline_configuration("router-pipeline")
```

### get_pipeline_nodes

Read node-level runtime status from a pipeline job.

```python
nodes = await client.get_pipeline_nodes(job_guid)
for node in nodes:
    print(node["id"], node["status"])
```

For the full pipeline JSON contract, node types, and branching examples, see [Pipeline Mode](/docs/reference/pipeline-mode).

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
    response = await client.upload_template(
        name="form-template",
        document=f.read(),
        file_name="form.pdf",
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

For PDF templates uploaded moments earlier, the raw endpoint may still be preparing metadata. In that case prefer `wait_for_template_metadata(...)`.

```python
meta = await client.metadata("form-template")
```

### wait_for_template_metadata

Wait until PDF template metadata is ready and return the decoded field list.

```python
fields = await client.wait_for_template_metadata(
    "form-template",
    timeout=60,
    poll_interval=1,
)
```

### fill

Fill a template with data.

```python
from docudevs.models import TemplateFillRequest

request = TemplateFillRequest(fields={"name": "John"})
response = await client.fill("form-template", request)
```

### fill_with_retry

Fill a template while retrying transient readiness errors that can happen immediately after upload.

```python
request = TemplateFillRequest(fields={"name": "John"})
response = await client.fill_with_retry(
    "form-template",
    request,
    timeout=30,
    poll_interval=1,
)
```

### delete_template

Delete a template.

```python
await client.delete_template("form-template")
```

## AcroForm PDF Metadata

### extract_acroform_metadata

Upload a PDF directly to `/document/acroform-metadata` and return the rich AcroForm metadata structure without creating a job GUID.

```python
metadata = await client.extract_acroform_metadata(
    document=pdf_bytes,
    file_name="fillable-form.pdf",
    mime_type="application/pdf",
)
```

### get_acroform_metadata

Fetch the stored AcroForm metadata artifact from an async processed job or a `pdf-acroform` operation job.

```python
job_guid = await client.submit_and_process_document(
    document=pdf_bytes,
    document_mime_type="application/pdf",
    acro_form_metadata=True,
)

metadata = await client.get_acroform_metadata(job_guid)
```

The direct helper is for metadata-only PDF uploads. The async helper is for workflows where the same job also needs source locations, rendered page images, extraction, or overlays.

For generated AcroForm PDFs, this metadata reflects the widgets embedded in the output PDF. Use `get_pdf_acroform_field_definitions(...)` when you need the normalized editable boxes that were used to generate those widgets.

## Agent Chat

### agent_chat

Send a chat message to the agent and receive a job GUID.

```python
response = await client.agent_chat(
    messages=[{"role": "user", "content": "Help me extract invoice line items"}],
    session_id="session-123"
)
```

### agent_status

Check the status of an agent chat job.

```python
status = await client.agent_status(response["jobGuid"])
```

### agent_chat_and_wait

Send a message and wait for the completed response.

```python
result = await client.agent_chat_and_wait(
    messages=[{"role": "user", "content": "Create a schema for insurance claims"}],
    session_id="session-123"
)
print(result["response"]["message"])
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

### get_document_summary

Get the AI-generated summary for a specific document in a case.

```python
result = await client.get_document_summary(case_id=123, document_id="doc-uuid")
summary = result.parsed
print(summary["filename"])  # "invoice.pdf"
print(summary["summary"]["document_type"])  # "invoice"
```

### list_case_summaries

List AI-generated summaries for all documents in a case.

```python
result = await client.list_case_summaries(case_id=123)
for doc in result.parsed:
    print(f"{doc['filename']}: {doc['summary']['document_type']}")
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

### submit_and_wait_for_image_selection

Select relevant figures from a completed job that extracted figures.

```python
selection = await client.submit_and_wait_for_image_selection(
    job_guid,
    prompt="Return all diagrams from the document",
    top_k=5,
    match_mode="all",
    use_vision=False
)

import json
selection_payload = json.loads(selection.result)
selected = selection_payload.get("selected", [])
```

### submit_pdf_acroform_operation

Queue `pdf-acroform` on a completed PDF job.

```python
response = await client.submit_pdf_acroform_operation(
    job_guid=parent_job_guid,
    llm_type="DEFAULT",
    ocr="PREMIUM",
    page_range=[1, 2],
    min_confidence=0.35,
    max_fields_per_page=300,
)

print(response.parsed["jobGuid"])
```

### submit_and_wait_for_pdf_acroform_operation

Run `pdf-acroform`, wait for completion, and download the generated fillable PDF.

```python
conversion = await client.submit_and_wait_for_pdf_acroform_operation(
    parent_job_guid,
    ocr="PREMIUM",
    min_confidence=0.35,
    max_fields_per_page=300,
    save_to="fillable.pdf",
)

print(conversion.operation_job_guid)
```

### get_pdf_acroform

Download the generated PDF bytes for a `pdf-acroform` or `pdf-acroform-apply` operation job.

```python
pdf_bytes = await client.get_pdf_acroform(operation_job_guid, save_to="fillable.pdf")
```

### get_pdf_acroform_field_definitions

Fetch the normalized editable field definitions for a `pdf-acroform` operation.

`entryBbox` uses normalized page-image coordinates from `0..1`, which makes this artifact the correct input for review tooling and manual edits.

```python
definitions = await client.get_pdf_acroform_field_definitions(operation_job_guid)
fields = definitions["fieldDefinitions"] if definitions else []
```

### submit_pdf_acroform_apply_operation

Queue reviewed field definitions to regenerate the PDF without rerunning visual detection.

```python
response = await client.submit_pdf_acroform_apply_operation(
    parent_job_guid,
    field_definitions=edited_fields,
    source_operation_guid=operation_job_guid,
)

print(response.parsed["jobGuid"])
```

### submit_and_wait_for_pdf_acroform_apply_operation

Submit reviewed field definitions, wait for completion, and download the regenerated PDF.

```python
applied = await client.submit_and_wait_for_pdf_acroform_apply_operation(
    parent_job_guid,
    field_definitions=edited_fields,
    source_operation_guid=operation_job_guid,
    save_to="fillable-reviewed.pdf",
)

print(applied.operation_job_guid)
```

## Map-Reduce Helpers

### submit_and_process_document_map_reduce

Process large documents using map-reduce strategy.

```python
job_guid = await client.submit_and_process_document_map_reduce(
    document=doc_bytes,
    document_mime_type="application/pdf",
    prompt="Extract line items",
    split_type="page",
    pages_per_chunk=5,
    overlap_pages=1,
    dedup_key="sku",
    parallel_processing=True
)
```

### submit_and_wait_for_map_reduce

Run map-reduce on an already-processed job without re-uploading or re-running OCR. This is the map-reduce equivalent of `submit_and_wait_for_generative_task`.

```python
# First: process a document normally (OCR runs here)
job_guid = await client.submit_and_process_document(
    document=doc_bytes,
    document_mime_type="application/pdf",
    prompt="Extract summary",
)
await client.wait_until_ready(job_guid)

# Later: re-run with map-reduce — no new upload, reuses OCR from parent job
result = await client.submit_and_wait_for_map_reduce(
    parent_job_id=job_guid,
    prompt="Extract all line items (sku, description, quantity, total)",
    schema='{"type":"array","items":{"type":"object"}}',
    split_type="page",
    pages_per_chunk=5,
    overlap_pages=1,
    dedup_key="sku",
    parallel_processing=True,
    timeout=300,
    result_format="json"
)
print(result["records"])
```

**Parameters:**

| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| `parent_job_id` | str | *required* | GUID of the completed job whose document to re-process. |
| `prompt` | str | `""` | Extraction instructions. |
| `schema` | str | `""` | JSON schema for structured extraction. |
| `split_type` | str | `"page"` | Chunk strategy: `"page"` or `"markdown_header"`. |
| `split_header_level` | int | `2` (markdown mode) | Header level (`1` or `2`) used when `split_type="markdown_header"`. |
| `pages_per_chunk` | int | `1` | Pages per chunk. |
| `overlap_pages` | int | `0` | Overlapping pages between chunks. |
| `dedup_key` | str | `None` | Required when `overlap_pages > 0`. |
| `parallel_processing` | bool | `False` | Run chunks in parallel. |
| `mime_type` | str | `"application/pdf"` | Document MIME type. |
| `timeout` | int | `180` | Max seconds to wait. |
| `poll_interval` | float | `5.0` | Seconds between status polls. |
| `result_format` | str | `"json"` | `"json"`, `"csv"`, `"excel"`, or `None`. |

When `split_type="markdown_header"`, `overlap_pages` and `dedup_key` are not supported.

All other map-reduce parameters (`header_options`, `header_schema`, `header_prompt`, `stop_when_empty`, `empty_chunk_grace`, `ocr`, `llm`, `trace`, `page_range`, `tools`, etc.) are also accepted.

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

### get_figures_metadata

Get extracted figure metadata for a job.

```python
metadata = await client.get_figures_metadata(job_guid)
images = metadata.get("images", []) if metadata else []
```

### get_figure_image

Download a figure image by ID.

```python
figure_id = images[0]["id"]
image_bytes = await client.get_figure_image(job_guid, figure_id)
if image_bytes:
    with open("figure_0.png", "wb") as f:
        f.write(image_bytes)
```

## Job Management

### delete_job

Delete a job and its associated data. Jobs must be in a terminal state (COMPLETED, ERROR, TIMEOUT, or PARTIAL). Jobs older than 14 days are automatically purged, so this method is primarily for cleaning up recent jobs.

```python
result = await client.delete_job(job_guid)
if result.status_code == 200:
    print(f"Deleted {result.parsed['jobsDeleted']} job(s)")
```

**Parameters:**

- `guid` (str): The job GUID to delete

**Returns:** A response object with:

- `status_code` (int): HTTP status (200 on success, 404 if not found)
- `parsed` (dict): Contains `jobsDeleted`, `errors` on success

**Note:** Deleting a job removes all associated data including uploaded documents, OCR results, and extracted data. Usage/billing records are preserved but disassociated from the deleted job.

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
